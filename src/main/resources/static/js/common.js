/* SmartBank browser client. The API remains JWT/stateless; this file only owns the UI. */
(function () {
    "use strict";

    const TOKEN_KEY = "token";
    const $ = (selector, root = document) => root.querySelector(selector);
    const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

    function token() {
        return sessionStorage.getItem(TOKEN_KEY);
    }

    function decodeToken(value = token()) {
        if (!value) return {};
        try {
            const payload = value.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
            return JSON.parse(atob(payload));
        } catch (_) {
            return {};
        }
    }

    function role() {
        return decodeToken().role || "CUSTOMER";
    }

    function logout() {
        sessionStorage.removeItem(TOKEN_KEY);
        window.location.assign("/login");
    }

    async function apiFetch(url, options = {}) {
        const headers = new Headers(options.headers || {});
        const jwt = token();
        if (jwt) headers.set("Authorization", `Bearer ${jwt}`);
        if (options.body && !(options.body instanceof FormData)) {
            headers.set("Content-Type", "application/json");
        }
        const response = await fetch(url, {...options, headers});
        let body = null;
        const contentType = response.headers.get("content-type") || "";
        if (response.status !== 204) {
            body = contentType.includes("application/json") ? await response.json() : await response.text();
        }
        if (response.status === 401) {
            sessionStorage.removeItem(TOKEN_KEY);
            window.location.assign("/login");
            throw new Error("Your session has expired. Please sign in again.");
        }
        if (!response.ok) {
            const message = body && typeof body === "object"
                ? (body.message || Object.values(body.details || {}).join(" ") || "Request failed.")
                : (body || "Request failed.");
            throw new Error(message);
        }
        return body;
    }

    const money = value => new Intl.NumberFormat("en-IN", {
        style: "currency", currency: "INR", maximumFractionDigits: 2
    }).format(Number(value || 0));
    const date = value => value ? new Date(value).toLocaleDateString("en-IN", {
        day: "2-digit", month: "short", year: "numeric"
    }) : "—";
    const dateTime = value => value ? new Date(value).toLocaleString("en-IN", {
        day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit"
    }) : "—";
    const title = value => String(value || "").toLowerCase().replaceAll("_", " ")
        .replace(/\b\w/g, letter => letter.toUpperCase());
    const escape = value => String(value ?? "").replace(/[&<>"']/g, char =>
        ({'&': "&amp;", '<': "&lt;", '>': "&gt;", '"': "&quot;", "'": "&#039;"}[char]));

    function toast(message, type = "success") {
        const host = $("#toast-host") || document.body;
        const item = document.createElement("div");
        item.className = `toast ${type}`;
        item.textContent = message;
        host.appendChild(item);
        setTimeout(() => item.remove(), 4500);
    }

    function setBusy(button, busy) {
        if (!button) return;
        button.disabled = busy;
        if (busy) {
            button.dataset.originalText = button.textContent;
            button.textContent = "Please wait…";
        } else if (button.dataset.originalText) {
            button.textContent = button.dataset.originalText;
        }
    }

    async function runForm(form, action) {
        const button = $("button[type=submit]", form);
        try {
            setBusy(button, true);
            await action(new FormData(form));
        } catch (error) {
            toast(error.message, "error");
        } finally {
            setBusy(button, false);
        }
    }

    function requireAuth() {
        if (!token()) {
            window.location.assign("/login");
            return false;
        }
        return true;
    }

    function renderNavbar() {
        const current = location.pathname;
        const admin = role() === "ADMIN";
        const navbar = $(".navbar");
        navbar?.classList.toggle("admin-mode", admin);
        navbar?.classList.toggle("customer-mode", !admin);
        $$(".customer-only").forEach(el => el.hidden = admin);
        $$(".admin-only").forEach(el => el.hidden = !admin);
        $$(".nav-link").forEach(link => link.classList.toggle("active", link.getAttribute("href") === current));
    }

    async function loginPage() {
        const form = $("#login-form");
        if (!form) return;
        if (token()) window.location.assign(role() === "ADMIN" ? "/admin/dashboard" : "/customer/dashboard");
        form.addEventListener("submit", event => {
            event.preventDefault();
            runForm(form, async data => {
                const result = await apiFetch("/api/auth/login", {
                    method: "POST",
                    body: JSON.stringify({email: data.get("email"), password: data.get("password")})
                });
                sessionStorage.setItem(TOKEN_KEY, result.token);
                window.location.assign(result.token && role() === "ADMIN" ? "/admin/dashboard" : "/customer/dashboard");
            });
        });
    }

    async function registerPage() {
        const form = $("#register-form");
        if (!form) return;
        form.addEventListener("submit", event => {
            event.preventDefault();
            runForm(form, async data => {
                await apiFetch("/api/auth/register", {
                    method: "POST",
                    body: JSON.stringify({
                        name: data.get("name"), email: data.get("email"),
                        phone: data.get("phone"), password: data.get("password")
                    })
                });
                toast("Account created. Sign in to continue.");
                setTimeout(() => window.location.assign("/login"), 900);
            });
        });
    }

    async function loadProfile(target = "#profile-name") {
        const customer = await apiFetch("/api/customers/me");
        const node = $(target);
        if (node) node.textContent = customer.name || "Customer";
        return customer;
    }

    async function dashboardPage() {
        if (!requireAuth()) return;
        try {
            const [customer, accounts, history] = await Promise.all([
                apiFetch("/api/customers/me"), apiFetch("/api/accounts"),
                apiFetch("/api/transactions?size=5")
            ]);
            $("#welcome-name").textContent = customer.name;
            $("#account-count").textContent = accounts.length;
            $("#total-balance").textContent = money(accounts.reduce((sum, account) => sum + Number(account.balance || 0), 0));
            $("#active-account").textContent = accounts.filter(account => account.status === "ACTIVE").length;
            const list = $("#recent-transactions");
            list.innerHTML = (history.content || []).map(transaction => `
                <tr><td>${title(transaction.transactionType)}</td>
                <td>${escape(transaction.reference || "—")}</td>
                <td class="${transaction.transactionType === "DEPOSIT" || transaction.transactionType === "TRANSFER_CREDIT" ? "positive" : ""}">
                    ${money(transaction.amount)}</td><td>${dateTime(transaction.transactionDate)}</td>
                <td><span class="status ${String(transaction.status).toLowerCase()}">${title(transaction.status)}</span></td></tr>`
            ).join("") || `<tr><td colspan="5" class="empty">No transactions yet.</td></tr>`;
        } catch (error) { toast(error.message, "error"); }
    }

    function accountMarkup(account) {
        return `<article class="card account-card">
            <div class="card-heading"><div><span class="eyebrow">${title(account.accountType)}</span>
                <h3>${escape(account.accountNumber)}</h3></div>
                <span class="status ${String(account.status).toLowerCase()}">${title(account.status)}</span></div>
            <div class="balance">${money(account.balance)}</div><p class="muted">Opened ${date(account.createdAt)}</p>
            <div class="inline-forms">
                <form class="account-action" data-action="deposit" data-id="${account.id}">
                    <input name="amount" type="number" min="0.01" step="0.01" placeholder="Amount" required>
                    <button class="button small" type="submit">Deposit</button></form>
                <form class="account-action" data-action="withdraw" data-id="${account.id}">
                    <input name="amount" type="number" min="0.01" step="0.01" placeholder="Amount" required>
                    <button class="button small secondary" type="submit">Withdraw</button></form>
            </div>
            <form class="transfer-form" data-id="${account.id}">
                <input name="toAccountNumber" inputmode="numeric" pattern="[0-9]{12}" placeholder="12-digit destination account" required>
                <input name="amount" type="number" min="0.01" step="0.01" placeholder="Amount" required>
                <button class="button small outline" type="submit">Transfer</button>
            </form></article>`;
    }

    async function accountsPage() {
        if (!requireAuth()) return;
        const host = $("#accounts-list");
        async function refresh() {
            const accounts = await apiFetch("/api/accounts");
            host.innerHTML = accounts.map(accountMarkup).join("") ||
                `<div class="empty-panel">No accounts yet. Open your first account below.</div>`;
            $$(".account-action", host).forEach(form => form.addEventListener("submit", event => {
                event.preventDefault();
                runForm(form, async data => {
                    await apiFetch(`/api/accounts/${form.dataset.id}/${form.dataset.action}`, {
                        method: "POST", body: JSON.stringify({amount: Number(data.get("amount"))})
                    });
                    toast(`${title(form.dataset.action)} completed.`);
                    await refresh();
                });
            }));
            $$(".transfer-form", host).forEach(form => form.addEventListener("submit", event => {
                event.preventDefault();
                runForm(form, async data => {
                    await apiFetch(`/api/accounts/${form.dataset.id}/transfer`, {
                        method: "POST", body: JSON.stringify({
                            toAccountNumber: data.get("toAccountNumber"), amount: Number(data.get("amount"))
                        })
                    });
                    toast("Transfer submitted.");
                    await refresh();
                });
            }));
        }
        try { await refresh(); } catch (error) { toast(error.message, "error"); }
        const openForm = $("#open-account-form");
        openForm?.addEventListener("submit", event => {
            event.preventDefault();
            runForm(openForm, async data => {
                await apiFetch("/api/accounts", {
                    method: "POST", body: JSON.stringify({accountType: data.get("accountType")})
                });
                openForm.reset(); toast("Account opened."); await refresh();
            });
        });
    }

    async function transactionsPage() {
        if (!requireAuth()) return;
        const form = $("#transaction-filters"), body = $("#transactions-list");
        async function refresh() {
            const params = new URLSearchParams({size: "20"});
            new FormData(form).forEach((value, key) => { if (value) params.set(key, value); });
            const page = await apiFetch(`/api/transactions?${params}`);
            body.innerHTML = (page.content || []).map(item => `<tr>
                <td>${dateTime(item.transactionDate)}</td><td>${title(item.transactionType)}</td>
                <td>${escape(item.reference || "—")}</td><td>${money(item.amount)}</td>
                <td><span class="status ${String(item.status).toLowerCase()}">${title(item.status)}</span></td></tr>`
            ).join("") || `<tr><td colspan="5" class="empty">No matching transactions.</td></tr>`;
            $("#transaction-count").textContent = `${page.totalElements} transaction${page.totalElements === 1 ? "" : "s"}`;
        }
        form?.addEventListener("submit", event => { event.preventDefault(); refresh().catch(error => toast(error.message, "error")); });
        try { await refresh(); } catch (error) { toast(error.message, "error"); }
    }

    async function beneficiariesPage() {
        if (!requireAuth()) return;
        const host = $("#beneficiaries-list"), form = $("#beneficiary-form");
        async function refresh() {
            const list = await apiFetch("/api/beneficiaries");
            host.innerHTML = list.map(item => `<article class="list-item">
                <div><strong>${escape(item.name)}</strong><p class="muted">${escape(item.bankName)} · ${escape(item.accountNumber)}</p></div>
                <div><span class="status ${String(item.status).toLowerCase()}">${title(item.status)}</span>
                <button class="button text-button remove-beneficiary" data-id="${item.id}">Remove</button></div></article>`
            ).join("") || `<div class="empty-panel">Add a trusted recipient to make transfers easier.</div>`;
            $$(".remove-beneficiary", host).forEach(button => button.addEventListener("click", async () => {
                if (!confirm("Remove this beneficiary?")) return;
                try { await apiFetch(`/api/beneficiaries/${button.dataset.id}`, {method: "DELETE"}); toast("Beneficiary removed."); await refresh(); }
                catch (error) { toast(error.message, "error"); }
            }));
        }
        form?.addEventListener("submit", event => {
            event.preventDefault();
            runForm(form, async data => {
                await apiFetch("/api/beneficiaries", {method: "POST", body: JSON.stringify({
                    name: data.get("name"), accountNumber: data.get("accountNumber"), bankName: data.get("bankName")
                })});
                form.reset(); toast("Beneficiary added."); await refresh();
            });
        });
        try { await refresh(); } catch (error) { toast(error.message, "error"); }
    }

    function cardMarkup(card) {
        return `<article class="card bank-card">
            <div class="card-heading"><span class="chip">SMARTBANK</span>
                <span class="status ${String(card.status).toLowerCase()}">${title(card.status)}</span></div>
            <p class="masked-number">${escape(card.maskedCardNumber)}</p>
            <div class="card-heading"><div><span class="eyebrow">${title(card.cardType)} card</span>
                <strong>Expires ${date(card.expiryDate)}</strong></div><strong>${money(card.availableCredit)}</strong></div>
            <div class="card-meta"><span>Limit<br><b>${money(card.creditLimit)}</b></span>
                <span>Outstanding<br><b>${money(card.outstandingAmount)}</b></span></div>
            <div class="card-actions">
                ${card.status === "INACTIVE" ? `<button class="button small activate-card" data-id="${card.id}">Activate</button>` : ""}
                ${card.status === "ACTIVE" ? `<button class="button small secondary block-card" data-id="${card.id}">Block</button>` : ""}
                <button class="button small outline show-card-forms" data-id="${card.id}">Use card</button>
            </div>
            <div class="card-forms" id="card-forms-${card.id}" hidden>
                <form class="purchase-form" data-id="${card.id}"><input name="merchant" placeholder="Merchant" required>
                    <input name="amount" type="number" min=".01" step=".01" placeholder="Amount" required>
                    <button class="button small" type="submit">Purchase</button></form>
                <form class="payment-form" data-id="${card.id}"><select name="sourceAccountId" required><option value="">Pay from account…</option></select>
                    <input name="amount" type="number" min=".01" step=".01" placeholder="Payment" required>
                    <button class="button small secondary" type="submit">Pay card</button></form>
            </div></article>`;
    }

    async function cardsPage() {
        if (!requireAuth()) return;
        const host = $("#cards-list"), requestForm = $("#card-request-form");
        let accounts = [];
        async function refresh() {
            const [cards, accountList] = await Promise.all([apiFetch("/api/cards"), apiFetch("/api/accounts")]);
            accounts = accountList;
            host.innerHTML = cards.map(cardMarkup).join("") || `<div class="empty-panel">No cards yet. Request one to get started.</div>`;
            $$(".payment-form select", host).forEach(select => select.innerHTML += accounts.map(a =>
                `<option value="${a.id}">${escape(a.accountNumber)} · ${money(a.balance)}</option>`).join(""));
            $$(".activate-card", host).forEach(button => cardAction(button, "activate"));
            $$(".block-card", host).forEach(button => cardAction(button, "block"));
            $$(".show-card-forms", host).forEach(button => button.addEventListener("click", () => {
                $(`#card-forms-${button.dataset.id}`).hidden = !$(`#card-forms-${button.dataset.id}`).hidden;
            }));
            $$(".purchase-form", host).forEach(form => form.addEventListener("submit", event => {
                event.preventDefault(); runForm(form, async data => {
                    await apiFetch(`/api/cards/${form.dataset.id}/purchase`, {method: "POST", body: JSON.stringify({
                        merchant: data.get("merchant"), amount: Number(data.get("amount"))
                    })}); toast("Purchase recorded."); await refresh();
                });
            }));
            $$(".payment-form", host).forEach(form => form.addEventListener("submit", event => {
                event.preventDefault(); runForm(form, async data => {
                    await apiFetch(`/api/cards/${form.dataset.id}/payments`, {method: "POST", body: JSON.stringify({
                        sourceAccountId: Number(data.get("sourceAccountId")), amount: Number(data.get("amount"))
                    })}); toast("Card payment submitted."); await refresh();
                });
            }));
        }
        function cardAction(button, action) {
            button.addEventListener("click", async () => {
                try {
                    await apiFetch(`/api/cards/${button.dataset.id}/${action}`, {method: "PUT"});
                    toast(`Card ${action === "block" ? "blocked" : "activated"}.`);
                    await refresh();
                }
                catch (error) { toast(error.message, "error"); }
            });
        }
        requestForm?.addEventListener("submit", event => {
            event.preventDefault(); runForm(requestForm, async data => {
                await apiFetch("/api/cards/request", {method: "POST", body: JSON.stringify({cardType: data.get("cardType")})});
                requestForm.reset(); toast("Card requested."); await refresh();
            });
        });
        try { await refresh(); } catch (error) { toast(error.message, "error"); }
    }

    async function aiPage() {
        if (!requireAuth()) return;
        const form = $("#ai-form"), answer = $("#ai-answer");
        form?.addEventListener("submit", event => {
            event.preventDefault(); runForm(form, async data => {
                answer.hidden = false; answer.textContent = "Thinking…";
                const result = await apiFetch("/api/ai/ask", {method: "POST", body: JSON.stringify({question: data.get("question")})});
                answer.textContent = result.answer;
            });
        });
    }

    async function profilePage() {
        if (!requireAuth()) return;
        try {
            const customer = await apiFetch("/api/customers/me");
            ["name", "email", "phone", "role"].forEach(key => {
                const node = $(`#profile-${key}`);
                if (node) node.textContent = customer[key.toLowerCase()] || customer[key] || "—";
            });
        } catch (error) { toast(error.message, "error"); }
    }

    async function adminPage() {
        if (!requireAuth() || role() !== "ADMIN") {
            if (token()) window.location.assign("/customer/dashboard");
            return;
        }
        try {
            const [dashboard, statistics, recent] = await Promise.all([
                apiFetch("/api/admin/dashboard"), apiFetch("/api/admin/statistics"),
                apiFetch("/api/admin/recent-transactions?limit=10")
            ]);
            $("#admin-customers").textContent = dashboard.totalCustomers;
            $("#admin-accounts").textContent = dashboard.totalAccounts;
            $("#admin-balance").textContent = money(dashboard.totalBalance);
            $("#admin-cards").textContent = dashboard.totalCards;
            $("#admin-outstanding").textContent = money(dashboard.totalCardOutstanding);
            $("#admin-today").textContent = dashboard.transactionsToday;
            $("#admin-transactions").innerHTML = recent.map(item => `<tr>
                <td>${dateTime(item.transactionDate)}</td><td>${title(item.transactionType)}</td>
                <td>${escape(item.maskedAccountNumber)}</td><td>${money(item.amount)}</td>
                <td><span class="status ${String(item.status).toLowerCase()}">${title(item.status)}</span></td>
            </tr>`).join("") || `<tr><td colspan="5" class="empty">No transactions found.</td></tr>`;
            $("#type-stats").innerHTML = statistics.transactionsByType.map(item =>
                `<div class="stat-row"><span>${title(item.type)}</span><strong>${item.count} · ${money(item.totalAmount)}</strong></div>`).join("");
            $("#daily-stats").innerHTML = statistics.transactionsLast7Days.map(item =>
                `<div class="stat-row"><span>${date(item.date)}</span><strong>${item.count} · ${money(item.totalAmount)}</strong></div>`).join("");
            $("#account-stats").innerHTML = statistics.accountsByType.map(item =>
                `<div class="stat-row"><span>${title(item.label)}</span><strong>${item.count}</strong></div>`).join("");
            $("#card-stats").innerHTML = statistics.cardsByStatus.map(item =>
                `<div class="stat-row"><span>${title(item.label)}</span><strong>${item.count}</strong></div>`).join("");
        } catch (error) { toast(error.message, "error"); }
    }

    document.addEventListener("DOMContentLoaded", () => {
        renderNavbar();
        if ($("#login-form")) loginPage();
        if ($("#register-form")) registerPage();
        if (document.body.dataset.page === "dashboard") dashboardPage();
        if (document.body.dataset.page === "accounts") accountsPage();
        if (document.body.dataset.page === "transactions") transactionsPage();
        if (document.body.dataset.page === "beneficiaries") beneficiariesPage();
        if (document.body.dataset.page === "cards") cardsPage();
        if (document.body.dataset.page === "ai") aiPage();
        if (document.body.dataset.page === "profile") profilePage();
        if (document.body.dataset.page === "admin") adminPage();
        $$(".logout").forEach(button => button.addEventListener("click", logout));
    });

    window.SmartBank = {apiFetch, logout, money, title, toast};
})();
