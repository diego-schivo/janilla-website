/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.website;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;

import com.janilla.io.IO;
import com.janilla.json.Converter;
import com.janilla.json.Json;
import com.janilla.persistence.ApplicationPersistenceBuilder;
import com.janilla.persistence.Persistence;
import com.janilla.reflect.Factory;

public class CustomPersistenceBuilder extends ApplicationPersistenceBuilder {

	public Properties configuration;

	public CustomPersistenceBuilder(Path databaseFile, Factory factory) {
		super(databaseFile, factory);
	}

	@Override
	public Persistence build() {
		var fe = Files.exists(databaseFile);
		var p = super.build();
		if (!fe)
			try {
				SeedData sd;
				try (var is = getClass().getResourceAsStream("seed-data.json")) {
					var s = new String(is.readAllBytes());
					var o = Json.parse(s);
					sd = (SeedData) factory.create(Converter.class).convert(o, SeedData.class);
				}
				for (var x : sd.media())
					persistence.crud(Media.class).create(x);
				for (var x : sd.users())
					persistence.crud(User.class).create(x);
				for (var x : sd.examples())
					persistence.crud(Example.class).create(x);
				persistence.crud(Header.class).create(sd.header());
				persistence.crud(Page.class).create(sd.page());
				persistence.crud(Footer.class).create(sd.footer());

				var r = getClass().getResource("seed-data.zip");
				URI u;
				try {
					u = r.toURI();
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}
				if (!u.toString().startsWith("jar:"))
					u = URI.create("jar:" + u);
				var s = IO.zipFileSystem(u).getPath("/");
//				var d = Files.createDirectories(databaseFile.getParent().resolve("janilla-website-upload"));
				var ud = configuration.getProperty("janilla-website.upload.directory");
				if (ud.startsWith("~"))
					ud = System.getProperty("user.home") + ud.substring(1);
				var d = Files.createDirectories(Path.of(ud));
				Files.walkFileTree(s, new SimpleFileVisitor<>() {

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						var t = d.resolve(s.relativize(file).toString());
						Files.copy(file, t, StandardCopyOption.REPLACE_EXISTING);
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		return p;
	}
}
