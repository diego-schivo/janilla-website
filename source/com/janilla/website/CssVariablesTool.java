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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CssVariablesTool {

	public static void main(String[] args) {
		var a = "git/janilla-ecommerce-template/frontend/source/com/janilla/ecommercetemplate/frontend";
//		var a = "git/janilla-admin-frontend/source/com/janilla/admin/frontend";
		var b = "style.css";
//		var b = "admin.css";
		try {
			var d = Path.of(System.getProperty("user.home")).resolve(a);
			var nn = Files.lines(d.resolve(b)).filter(x -> x.startsWith("@import "))
					.map(x -> x.substring(x.indexOf('"') + 1, x.lastIndexOf('"')))
					.filter(x -> Files.exists(d.resolve(x))).toList();

			var m1 = nn.stream().map(d::resolve).collect(Collectors.toMap(x -> x, x -> {
				try {
					return Files.readAllLines(x);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}, (x, _) -> x, LinkedHashMap::new));

			var ll0 = m1.values().iterator().next();
			var ii = IntStream.range(0, ll0.size()).filter(x -> ll0.get(x).trim().startsWith("--")).toArray();
			var m2 = Arrays.stream(ii).mapToObj(x -> ll0.get(x).split(":"))
					.collect(Collectors.toMap(x -> x[0].trim(), x -> x[1].trim().replace(";", "")));
			for (var i = ii.length - 1; i >= 0; i--)
				ll0.remove(ii[i]);

			for (var ll : m1.values()) {
				for (var i = 0; i < ll.size(); i++) {
					var ss = ll.get(i).split(":");
					if (ss.length == 2) {
						var s0 = ss[0].trim();
						if (Set.of("background", "border", "color").contains(s0)) {
							var v = ss[1].trim().replace(";", "");
							if (v.contains("var(")) {
								var i2 = v.indexOf("var(") + "var(".length();
								var k = v.substring(i2, v.indexOf(')', i2));
								ll.set(i, ss[0].substring(0, ss[0].indexOf(s0)) + s0 + ": "
										+ v.replace("var(" + k + ")", m2.get(k)) + ";");
							}
						}
					}
				}
			}

			m2.clear();

			for (var ll : m1.values()) {
				for (var i = 0; i < ll.size(); i++) {
					var ss = ll.get(i).split(":");
					if (ss.length == 2) {
						var s0 = ss[0].trim();
						if (Set.of("background", "border", "color").contains(s0)) {
							var v = ss[1].trim().replace(";", "");
							var v2 = v.substring(
									(v.contains("(") ? v.lastIndexOf(' ', v.indexOf('(')) : v.lastIndexOf(' ')) + 1);
							if (!Set.of("0", "inherit", "none", "transparent").contains(v2)) {
								for (var i2 = 1;; i2++) {
									var k = "--" + s0 + "-" + i2;
									var v3 = m2.get(k);
									if (v3 == null || v3.equals(v2)) {
										if (v3 == null)
											m2.put(k, v2);
										ll.set(i, ss[0].substring(0, ss[0].indexOf(s0)) + s0 + ": "
												+ v.replace(v2, "var(" + k + ")") + ";");
										break;
									}
								}
							}
						}
					}
				}
			}

			var i = 0;
			for (var k : (Iterable<String>) (() -> m2.keySet().stream()
					.sorted(Comparator.comparing((String x) -> x.substring(0, x.lastIndexOf('-')))
							.thenComparingInt(x -> Integer.parseInt(x.substring(x.lastIndexOf('-') + 1))))
					.iterator()))
				ll0.add(ii[0] + i++, "  " + k + ": " + m2.get(k) + ";");

			for (var e : m1.entrySet())
				Files.writeString(e.getKey(), String.join("\n", e.getValue()) + "\n");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
