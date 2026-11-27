package com.greendelta.bioheating.examples;

import com.greendelta.bioheating.graph.Graph;
import com.greendelta.bioheating.graph.SteinerTree;
import com.greendelta.bioheating.model.Database;
import com.greendelta.bioheating.model.Project;
import com.sun.management.GarbageCollectionNotificationInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import org.openlca.commons.Res;

/// Benchmark of parallel calculations with the projects of the local database
/// (see `DatabaseExample`); only projects with a supply center and at least 3
/// heated buildings are used. For each number of parallel users, every user
/// calculates all these projects, each user starting with a different one. A
/// calculation is the same as in the calculation task of the server (load the
/// project, build the graph, compute the Steiner tree), but the solution is
/// not stored, so that the database is not modified.
///
/// The heap numbers depend on the heap size, so run it with the heap of the
/// server; in the Docker setup this is 25% of the container memory limit, e.g.
/// `-Xmx640m` for `mem_limit: 2560m`.
public class CalculationBenchmark {

	private static final int[] USERS = {1, 2, 3, 5, 10};

	/// The threads per calculation, as `app.calculation.threads`; 0 = auto.
	private static final int THREADS = 0;

	static void main(String[] args) throws Exception {
		var db = Database.of("bioheating")
			.withUser("postgres", "bioheating")
			.withHost("localhost", 5432)
			.connect();
		try (db) {
			var ids = db.getAll(Project.class).stream()
				.filter(CalculationBenchmark::isCalculable)
				.map(Project::id)
				.sorted()
				.toList();
			if (ids.isEmpty()) {
				System.out.println("No calculable projects in the database");
				return;
			}
			System.out.printf(
				"%d projects, %d CPUs, max. heap %d MB%n",
				ids.size(),
				Runtime.getRuntime().availableProcessors(),
				Runtime.getRuntime().maxMemory() / MB);

			System.out.println("Warm-up ...");
			calculate(db, ids.getFirst());

			var rows = new ArrayList<Row>();
			for (int users : USERS) {
				System.out.printf("Run with %d user(s) ...%n", users);
				rows.add(run(db, ids, users));
			}
			print(rows);
		}
	}

	/// Returns `true` if the project has a supply center and at least 3 heated
	/// buildings (heated and included, as in the graph of the calculation).
	private static boolean isCalculable(Project project) {
		var map = project.map();
		if (map == null) return false;
		boolean hasSupplyCenter = false;
		int heated = 0;
		for (var b : map.buildings()) {
			if (b.isSupplyCenter()) {
				hasSupplyCenter = true;
			} else if (b.isHeated() && b.isIncluded()) {
				heated++;
			}
		}
		return hasSupplyCenter && heated >= 3;
	}

	private static Row run(Database db, List<Long> ids, int users)
		throws Exception {
		System.gc();
		var memory = new MemoryMonitor();
		var times = new ConcurrentLinkedQueue<Long>();
		var errors = new ConcurrentLinkedQueue<String>();

		long start = System.nanoTime();
		try (var pool = Executors.newFixedThreadPool(users)) {
			for (int u = 0; u < users; u++) {
				int offset = u;
				pool.submit(() -> {
					for (int i = 0; i < ids.size(); i++) {
						long id = ids.get((offset + i) % ids.size());
						long t = System.nanoTime();
						var res = calculate(db, id);
						times.add(System.nanoTime() - t);
						if (res.isError()) {
							errors.add("project " + id + ": " + res.error());
						}
					}
				});
			}
		} // close() waits for all tasks
		long total = System.nanoTime() - start;
		memory.close();

		for (var e : errors) {
			System.out.println("  error: " + e);
		}
		long max = times.stream().mapToLong(Long::longValue).max().orElse(0);
		double avg = times.stream().mapToLong(Long::longValue).average().orElse(0);
		return new Row(
			users, times.size(), errors.size(), total, (long) avg, max,
			memory.baseline, memory.peak.get(), memory.peakAfterGc.get());
	}

	private static Res<?> calculate(Database db, long projectId) {
		try {
			var project = db.getForId(Project.class, projectId);
			if (project == null)
				return Res.error("project not found");
			var graph = Graph.buildFrom(project);
			if (graph.isError())
				return graph;
			return SteinerTree.compute(graph.value(), THREADS);
		} catch (Exception e) {
			return Res.error("calculation failed", e);
		}
	}

	private static void print(List<Row> rows) {
		System.out.println();
		System.out.println(
			"| Users | Calcs | Errors | Total [s] | Avg [s] | Max [s] "
				+ "| Heap start [MB] | Heap peak [MB] | After GC max [MB] |");
		System.out.println(
			"|------:|------:|-------:|----------:|--------:|--------:"
				+ "|----------------:|---------------:|------------------:|");
		for (var r : rows) {
			System.out.printf(
				"| %5d | %5d | %6d | %9.1f | %7.1f | %7.1f | %15d | %14d | %17d |%n",
				r.users, r.calcs, r.errors,
				r.totalNs / 1e9, r.avgNs / 1e9, r.maxNs / 1e9,
				r.heapStart / MB, r.heapPeak / MB, r.heapAfterGc / MB);
		}
	}

	private static final long MB = 1024 * 1024;

	private record Row(
		int users,
		int calcs,
		int errors,
		long totalNs,
		long avgNs,
		long maxNs,
		long heapStart,
		long heapPeak,
		long heapAfterGc
	) {}

	/// Tracks the heap during a run: `peak` is the maximum of the sampled used
	/// heap (including garbage that is not collected yet), and `peakAfterGc` is
	/// the maximum used heap directly after a garbage collection, which is
	/// closer to the memory that the calculations really need.
	private static class MemoryMonitor implements NotificationListener {

		final long baseline;
		final AtomicLong peak = new AtomicLong();
		final AtomicLong peakAfterGc = new AtomicLong();

		private final Set<String> heapPools;
		private final List<NotificationEmitter> emitters = new ArrayList<>();
		private final Thread sampler;

		MemoryMonitor() {
			var mem = ManagementFactory.getMemoryMXBean();
			baseline = mem.getHeapMemoryUsage().getUsed();
			heapPools = ManagementFactory.getMemoryPoolMXBeans().stream()
				.filter(p -> p.getType() == MemoryType.HEAP)
				.map(MemoryPoolMXBean::getName)
				.collect(Collectors.toSet());

			for (var gc : ManagementFactory.getGarbageCollectorMXBeans()) {
				if (gc instanceof NotificationEmitter emitter) {
					emitter.addNotificationListener(this, null, null);
					emitters.add(emitter);
				}
			}

			sampler = Thread.ofPlatform().daemon().start(() -> {
				while (!Thread.currentThread().isInterrupted()) {
					peak.accumulateAndGet(
						mem.getHeapMemoryUsage().getUsed(), Math::max);
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						return;
					}
				}
			});
		}

		@Override
		public void handleNotification(Notification n, Object handback) {
			if (!GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION
				.equals(n.getType()))
				return;
			var info = GarbageCollectionNotificationInfo.from(
				(CompositeData) n.getUserData());
			long used = 0;
			var afterGc = info.getGcInfo().getMemoryUsageAfterGc();
			for (var e : afterGc.entrySet()) {
				if (heapPools.contains(e.getKey())) {
					used += e.getValue().getUsed();
				}
			}
			peakAfterGc.accumulateAndGet(used, Math::max);
		}

		void close() throws Exception {
			sampler.interrupt();
			sampler.join();
			for (var emitter : emitters) {
				emitter.removeNotificationListener(this);
			}
		}
	}
}
