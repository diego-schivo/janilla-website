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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.stream.Collectors;

class FormatTool {

	public static void main(String[] args) throws Exception {
		int x = 80;
//			var f = Path.of(System.getProperty("user.home")).resolve( "Projects/janilla/LICENSE");
		var f = Path.of(System.getProperty("user.home")).resolve("Projects/janilla/README");
		var l = Files.readAllLines(f);
		var m = new LinkedList<String>();
		var n = 0;
		StringBuilder b = null;
		for (var s : l) {
			if (b == null)
				b = new StringBuilder(s);
			else if (s.isEmpty() || b.isEmpty()) {
				m.add(b.toString());
				b.setLength(0);
				b.append(s);
			} else {
				var t = s.stripLeading();
				var i = t.indexOf(' ');
				if (i < 0)
					i = t.length();
				if (n + 1 + i >= x) {
					b.append(' ').append(t);
				} else {
					m.add(b.toString());
					b.setLength(0);
					b.append(s);
				}
			}
			n = s.length();
		}
//			if (!b.isEmpty()) {
		m.add(b.toString());
		b.setLength(0);
//			}
//			IO.println(m.stream().collect(Collectors.joining("\n")));
		l = new LinkedList<String>();
		for (var s : m) {
			n = 0;
			var p = "";
			for (var i = 0;;) {
				if (i + x - n >= s.length()) {
					l.add(p + (i == 0 ? s : s.substring(i)));
					break;
				}
				var j = s.lastIndexOf(' ', i + x - n - 1);
				if (j < i) {
					l.add(p + s.substring(i));
					break;
				}
				l.add(p + s.substring(i, j));
				if (i == 0) {
					for (; n < s.length() && s.charAt(n) == ' '; n++)
						;
					if (n > 0)
						p = " ".repeat(n);
				}
				i = j + 1;
			}
		}
//			IO.println(l.stream().collect(Collectors.joining("\n")));
//			f = Path.of(System.getProperty("user.home")).resolve( "Projects/janilla/LICENSE2");
		f = Path.of(System.getProperty("user.home")).resolve("Projects/janilla/README2");
//			Files.write(f, l);
		Files.writeString(f, l.stream().collect(Collectors.joining("\n")));
	}
}
