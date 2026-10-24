
const form = document.getElementById("expenseForm");
const message = document.getElementById("message");
const expenseList = document.getElementById("expenseList");
const totalElement = document.getElementById("total");
const countElement = document.getElementById("count");

async function loadExpenses() {
    try {
        const [transactionsResponse, summaryResponse] = await Promise.all([
            fetch("/api/transactions"),
            fetch("/api/summary")
        ]);

        if (!transactionsResponse.ok || !summaryResponse.ok) {
            throw new Error("Could not load expenses.");
        }

        const data = await transactionsResponse.json();
        const summary = await summaryResponse.json();

        expenseList.replaceChildren();

        if (data.transactions.length === 0) {
            const row = document.createElement("tr");
            const cell = document.createElement("td");
            cell.colSpan = 3;
            cell.textContent = "No expenses yet. Add your first expense!";
            row.appendChild(cell);
            expenseList.appendChild(row);
        } else {
            data.transactions.forEach(expense => {
                const row = document.createElement("tr");

                [expense.description, expense.category,
                 "₹" + Number(expense.amount).toFixed(2)]
                    .forEach(value => {
                        const cell = document.createElement("td");
                        cell.textContent = value;
                        row.appendChild(cell);
                    });

                expenseList.appendChild(row);
            });
        }

        totalElement.textContent = "₹" + Number(summary.total).toFixed(2);
        countElement.textContent = summary.count;
    } catch (error) {
        message.textContent = error.message +
            " Check that the Java server is running.";
    }
}

form.addEventListener("submit", async event => {
    event.preventDefault();
    message.textContent = "Saving expense...";

    try {
        const formData = new FormData(form);
        const response = await fetch("/api/add", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams(formData)
        });

        const result = await response.json();

        if (!response.ok || result.error) {
            throw new Error(result.error || "Could not save expense.");
        }

        form.reset();
        message.textContent = "Expense added successfully!";
        await loadExpenses();
    } catch (error) {
        message.textContent = "Error: " + error.message;
    }
});

document.getElementById("refreshButton")
    .addEventListener("click", loadExpenses);

loadExpenses();
