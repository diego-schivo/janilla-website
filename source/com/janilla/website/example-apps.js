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
import WebComponent from "web-component";

export default class ExampleApps extends WebComponent {

	static get templateNames() {
		return ["example-apps"];
	}

	static get observedAttributes() {
	    return ["data-path"];
	}

	constructor() {
		super();
	}

	async updateDisplay() {
		const o = this.closest("page-element").data(this.dataset.path);
		const ss = this.closest("app-element").serverState;
		this.appendChild(this.interpolateDom({
			$template: "",
			...o,
			examples: o.examples?.map(x => ({
				$template: "example",
				...x,
				links: [{
					$template: "link",
					uri: x.demo ? `https://${x.demo}.${ss.authority}` : "",
					target: "_blank",
					hidden: !x.demo,
					text: "Live demo"
				}, {
					$template: "link",
					uri: x.source ? `https://github.com/diego-schivo/${x.source}` : "",
					target: "_blank",
					hidden: !x.source,
					text: "Free source"
				}],
				media: x.video ? {
					$template: "video",
					uri: `https://www.youtube.com/embed/${x.video}`
				} : x.image ? {
					$template: "image",
					...x.image
				} : null
			}))
		}));
	}
}
