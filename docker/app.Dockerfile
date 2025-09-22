FROM eclipse-temurin:21-jre

WORKDIR /app
RUN apt-get update

# add libc and libgomp, these are required for XGBoost
RUN apt-get install -y libc6 libgomp1

COPY bioheating-tool.jar /app/bioheating-tool.jar

EXPOSE 3000

CMD ["java", "-XX:MaxRAMPercentage=80", "-XX:+UseG1GC", "-jar", "bioheating-tool.jar"]
