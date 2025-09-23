package com.greendelta.bioheating.examples;

import java.awt.Color;
import java.io.File;

import javax.imageio.ImageIO;

import org.locationtech.jts.geom.Envelope;

import com.greendelta.bioheating.Tests;
import com.greendelta.bioheating.io.GeoImage;
import com.greendelta.bioheating.model.Project;

public class GeoImageExample {
	public static void main(String[] args) {
		try (var db = Tests.db()) {
			var project = db.getAll(Project.class).getFirst();

			var envelope = new Envelope();
			for (var street : project.map().streets()) {
				for (var c : street.coordinates()) {
					envelope.expandToInclude(c);
				}
			}

			try (var img = new GeoImage(1024, 800, envelope)) {
				for (var street : project.map().streets()) {
					img.drawLine(street.coordinates(), Color.BLUE);
				}
				ImageIO.write(img.getImage(), "png", new File("target/streets.png"));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
