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
import { WebComponent } from "./web-component.js";

export default class Page extends WebComponent {

	static get observedAttributes() {
		return ["data-slug"];
	}

	static get templateName() {
		return "page";
	}

	constructor() {
		super();
	}

	async updateDisplay() {
		const r = this.closest("root-element");
		if (this.dataset.slug === "home") {
			const s = r.state;
			this.appendChild(this.interpolateDom({
				$template: "",
				layout: s.page?.layout?.map((x, i) => ({
					$template: x.$type.split(/(?=[A-Z])/).map(x => x.toLowerCase()).join("-"),
					expression: `layout.${i}`
				}))
			}));
		} else
			r.notFound();
	}

	data(expression) {
		return expression.split(".").reduce((x, y) => Array.isArray(x)
			? x[parseInt(y)]
			: typeof x === "object" && x !== null
				? x[y]
				: null, this.closest("root-element").state.page);
	}
}
