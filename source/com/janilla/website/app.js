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

const adminRegex = /^\/admin(\/.*)?$/;

export default class App extends WebComponent {

    static get templateNames() {
        return ["app"];
    }

    static get observedAttributes() {
        return ["data-api-url"];
    }

    constructor() {
        super();
        if (!history.state)
            history.replaceState({}, "");
    }

    get user() {
        return this.state.user;
    }

    set user(user) {
        this.state.user = user;
        this.dispatchEvent(new CustomEvent("userchanged", { detail: user }));
    }

    connectedCallback() {
        const el = this.children.length === 1 ? this.firstElementChild : null;
        if (el?.matches('[type="application/json"]')) {
            this.serverState = JSON.parse(el.text);
            el.remove();
        }
        super.connectedCallback();
        this.addEventListener("change", this.handleChange);
        this.addEventListener("click", this.handleClick);
        addEventListener("popstate", this.handlePopState);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
        this.removeEventListener("change", this.handleChange);
        this.removeEventListener("click", this.handleClick);
        removeEventListener("popstate", this.handlePopState);
    }

    async updateDisplay() {
        const s = this.state;
        const ss = this.serverState;

        if (ss?.error?.code === 404)
            s.notFound = true;

        if (!Object.hasOwn(s, "user"))
            s.user = ss && Object.hasOwn(ss, "user")
                ? ss.user
                : await (await fetch(`${this.dataset.apiUrl}/users/me`)).json();

        const p = location.pathname;
        const spp = new URLSearchParams(location.search);
        const m = p.match(adminRegex);
        if (m) {
            this.appendChild(this.interpolateDom({
                $template: "",
                admin: {
                    $template: "admin",
                    user: s.user ? JSON.stringify(s.user) : null,
                    path: m[1] ?? "/"
                }
            }));
            return;
        }

        if (!Object.hasOwn(s, "header"))
            s.header = ss && Object.hasOwn(ss, "header")
                ? ss.header
                : await (await fetch(`${this.dataset.apiUrl}/header`)).json();
        if (!Object.hasOwn(s, "footer"))
            s.footer = ss && Object.hasOwn(ss, "footer")
                ? ss.footer
                : await (await fetch(`${this.dataset.apiUrl}/footer`)).json();

        this.appendChild(this.interpolateDom({
            $template: "",
            public: {
                $template: "public",
				header: s.header ? {
					$template: "header",
					navItems: s.header.navItems?.map(x => ({
						$template: "link",
						...x,
						target: x.newTab ? "_blank" : null
					}))
				} : null,
                content: s.notFound ? { $template: "not-found" } : { $template: "page" },
				footer: s.footer ? {
					$template: "footer",
					...s.footer
				} : null
            }
        }));
    }

    handleClick = event => {
        const a = event.target.closest("a");
        if (a?.href && !event.defaultPrevented && !a.target) {
            const u = new URL(a.href);
            event.preventDefault();
            history.pushState({}, "", u.pathname + u.search);
            dispatchEvent(new CustomEvent("popstate"));
        }
    }

    handlePopState = () => {
        // console.log("handlePopState", JSON.stringify(history.state));
        delete this.serverState;
        window.scrollTo(0, 0);
        delete this.state.notFound;
        this.requestDisplay();
    }

    navigate(url) {
        delete this.serverState;
        delete this.state.notFound;
        if (url.pathname !== location.pathname)
            window.scrollTo(0, 0);
        history.pushState({}, "", url.pathname + url.search);
        this.requestDisplay();
    }

    notFound() {
        this.state.notFound = true;
        this.requestDisplay();
    }
}
