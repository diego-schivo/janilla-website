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

export default class ExampleApps extends WebComponent {

	static get templateName() {
		return "example-apps";
	}

	constructor() {
		super();
	}

	async updateDisplay() {
		const o = this.closest("page-element").data(this.dataset.expression);
		this.appendChild(this.interpolateDom({
			$template: "",
			...o,
			items: o.items.map(x => ({
				$template: "item",
				...x,
				links: x.links.map(y => ({
					$template: "link",
					...y,
					target: y.newTab ? "_blank" : null,
					hidden: y.uri === "#"
				})),
				media: x.video ? {
					$template: "video",
					uri: x.video
				} : x.image ? {
					$template: "image",
					...x.image
				} : null
			}))
		}));
	}
}
