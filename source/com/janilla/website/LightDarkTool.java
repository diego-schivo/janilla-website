/*
 * MIT License
 *
 * Copyright (c) 2024-2026 Diego Schivo
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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class LightDarkTool {

	public static void main(String[] args) throws Exception {
//		var h = Map.of("janilla-acme-dashboard", "", "janilla-address-book", "", "janilla-ide", "");
//		var h = Map.of("janilla", "", "janilla-templates", "");
//		var h = Map.of("janilla-address-book", "");
		var h = Map.of("janilla-ecommerce-template", "");
		var s = Path.of(System.getProperty("user.home")).resolve("git");
		Files.walkFileTree(s, new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				var n = dir.getFileName().toString();
				if (dir.getParent().equals(s)) {
					if (!h.containsKey(n))
						return FileVisitResult.SKIP_SUBTREE;
				}
				if (n.startsWith(".") || n.equals("src") || n.equals("bin") || n.equals("target") || n.equals("node")
						|| n.equals("node_modules"))
					return FileVisitResult.SKIP_SUBTREE;
				return FileVisitResult.CONTINUE;
			}

			protected static final Pattern COLOR = Pattern.compile("\\b(background(-color)?|color): (.*?);");

			protected static final Pattern HEX = Pattern.compile("#([0-9a-fA-F]{6})");

			protected static final Pattern RGB = Pattern.compile("rgb\\((\\d+), (\\d+), (\\d+)\\)");

			protected static final Pattern RGBA = Pattern.compile("rgba\\((\\d+), (\\d+), (\\d+), (\\\\d+)\\)");

			protected static final Map<String, String> NAMES = Map.of("black", "white", "darkgray", "lightgray",
					"lightgray", "darkgray", "white", "black");

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				var n = file.getFileName().toString();
				if (!n.endsWith(".css"))
					return FileVisitResult.CONTINUE;
//				IO.println(n);
				var ll = Files.readAllLines(file);
				var ll2 = ll.stream().map(x -> COLOR.matcher(x).replaceFirst(y -> {
					String s = null;
					var m1 = HEX.matcher(y.group(3));
					if (m1.matches()) {
						var i1 = Integer.parseInt(m1.group(1), 16);
						var bb1 = new byte[] { (byte) (i1 >>> 16), (byte) (i1 >>> 8), (byte) i1 };
						var bb2 = Arrays.copyOf(bb1, bb1.length);
						for (var i = 0; i < bb2.length; i++)
							bb2[i] = (byte) (0xff - bb2[i]);
						s = "#" + HexFormat.of().formatHex(bb2) + ", #" + HexFormat.of().formatHex(bb1);
					}
					var m2 = RGB.matcher(y.group(3));
					if (m2.matches()) {
						var ii1 = IntStream.range(0, 3).map(z -> Integer.parseInt(m2.group(z + 1))).toArray();
						var ii2 = Arrays.stream(ii1).map(z -> 255 - z).toArray();
						s = Stream.of(ii2, ii1)
								.map(z -> "rgb("
										+ Arrays.stream(z).mapToObj(String::valueOf).collect(Collectors.joining(", "))
										+ ")")
								.collect(Collectors.joining(", "));
					}
					var m3 = RGBA.matcher(y.group(3));
					if (m3.matches()) {
						var ii1 = IntStream.range(0, 3).map(z -> Integer.parseInt(m2.group(z + 1))).toArray();
						var ii2 = Arrays.stream(ii1).map(z -> 255 - z).toArray();
						s = Stream.of(ii2, ii1)
								.map(z -> "rgba("
										+ Arrays.stream(z).mapToObj(String::valueOf).collect(Collectors.joining(", "))
										+ ", " + m2.group(4) + ")")
								.collect(Collectors.joining(", "));
					}
					var n2 = NAMES.get(y.group(3));
					if (n2 != null)
						s = n2 + ", " + y.group(3);
					return s != null ? y.group(1) + ": light-dark(" + s + ");" : y.group();
				})).toList();
				if (!ll2.equals(ll)) {
					IO.println(file);
					Files.write(file, ll2);
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
