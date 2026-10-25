
const SUPABASE_URL = "https://wionrrmcikvxshbmpkpe.supabase.co";
const SUPABASE_KEY = "sb_publishable_li2EgaSELVMPAbisfOLbqA_9bW7tX5a";

const db = window.supabase.createClient(SUPABASE_URL, SUPABASE_KEY);

const authForm = document.getElementById("authForm");
const authTitle = document.getElementById("authTitle");
const authButton = document.getElementById("authButton");
const authMessage = document.getElementById("authMessage");
const toggleAuth = document.getElementById("toggleAuth");
const authSection = document.getElementById("authSection");
const trackerSection = document.getElementById("trackerSection");

const form = document.getElementById("expenseForm");
const message = document.getElementById("message");
const expenseList = document.getElementById("expenseList");
const totalElement = document.getElementById("total");
const countElement = document.getElementById("count");

let isSignUp = true;

toggleAuth.addEventListener("click", () => {
    isSignUp = !isSignUp;
    authTitle.textContent = isSignUp ? "Create Your Account" : "Log In";
    authButton.textContent = isSignUp ? "Sign Up" : "Log In";
    toggleAuth.textContent = isSignUp
        ? "Already have an account? Log in"
        : "New here? Create an account";
    authMessage.textContent = "";
});

authForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    authButton.disabled = true;
    authMessage.textContent = "Please wait...";

    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;

    try {
        const result = isSignUp
            ? await db.auth.signUp({ email, password })
            : await db.auth.signInWithPassword({ email, password });

        if (result.error) throw result.error;

        if (isSignUp && !result.data.session) {
            authMessage.textContent =
                "Account created! Check your email to confirm it, then log in.";
        } else {
            authForm.reset();
            authMessage.textContent = "";
            await showTracker(result.data.user);
        }
    } catch (error) {
        authMessage.textContent = error.message;
    } finally {
        authButton.disabled = false;
    }
});

async function showTracker(user) {
    if (!user) {
        authSection.hidden = false;
        trackerSection.hidden = true;
        return;
    }

    authSection.hidden = true;
    trackerSection.hidden = false;
    document.getElementById("userEmail").textContent = user.email || "";
    await loadExpenses();
}

document.getElementById("logoutButton").addEventListener("click", async () => {
    const { error } = await db.auth.signOut();
    if (error) {
        message.textContent = error.message;
        return;
    }

    expenseList.replaceChildren();
    totalElement.textContent = "₹0.00";
    countElement.textContent = "0";
    message.textContent = "";
    authForm.reset();
    await showTracker(null);
});

async function loadExpenses() {
    message.textContent = "Loading expenses...";

    const { data, error } = await db
        .from("expenses")
        .select("id, description, amount, category, created_at")
        .order("created_at", { ascending: false });

    if (error) {
        message.textContent = "Could not load expenses: " + error.message;
        return;
    }

    expenseList.replaceChildren();
    let total = 0;

    if (data.length === 0) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = 3;
        cell.textContent = "No expenses yet. Add your first expense!";
        row.appendChild(cell);
        expenseList.appendChild(row);
    }

    data.forEach((expense) => {
        const row = document.createElement("tr");

        [expense.description, expense.category,
            "₹" + Number(expense.amount).toFixed(2)].forEach((value) => {
            const cell = document.createElement("td");
            cell.textContent = value;
            row.appendChild(cell);
        });

        expenseList.appendChild(row);
        total += Number(expense.amount);
    });

    totalElement.textContent = "₹" + total.toFixed(2);
    countElement.textContent = data.length;
    message.textContent = "";
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    message.textContent = "Saving expense...";

    const { data: { user }, error: userError } = await db.auth.getUser();

    if (userError || !user) {
        message.textContent = "Please log in again.";
        await showTracker(null);
        return;
    }

    const formData = new FormData(form);
    const amount = Number(formData.get("amount"));

    if (!Number.isFinite(amount) || amount <= 0) {
        message.textContent = "Enter an amount greater than zero.";
        return;
    }

    const expense = {
        user_id: user.id,
        description: String(formData.get("description")).trim(),
        amount,
        category: String(formData.get("category"))
    };

    const { error } = await db.from("expenses").insert(expense);

    if (error) {
        message.textContent = "Could not save expense: " + error.message;
        return;
    }

    form.reset();
    message.textContent = "Expense added successfully!";
    await loadExpenses();
});

document.getElementById("refreshButton")
    .addEventListener("click", loadExpenses);

async function startApp() {
    const { data, error } = await db.auth.getSession();

    if (error) {
        authMessage.textContent = error.message;
        return;
    }

    await showTracker(data.session?.user || null);
}

db.auth.onAuthStateChange((event, session) => {
    if (event === "SIGNED_IN") {
        showTracker(session?.user || null);
    }
});

startApp();
