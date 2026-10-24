
#include <iostream>
#include <fstream>
#include <string>
#include <iomanip>
#include <cstdlib>
#include <limits>
using namespace std;

const string FILE_NAME = "transactions.txt";

string escapeJson(const string& s) {
    string result;
    for (char c : s) {
        if (c == '"' || c == '\\') result += '\\';
        if (c == '\n') result += "\\n";
        else result += c;
    }
    return result;
}

class Transaction {
public:
    int id;
    string description, category;
    double amount;

    Transaction(int i, string d, double a, string c)
        : id(i), description(d), category(c), amount(a) {}
};

class ExpenseTracker {
public:
    void add(string description, double amount, string category) {
        int id = 1;
        ifstream in(FILE_NAME);
        int oldId;
        string d, c;
        double a;

        while (in >> oldId) {
            in.ignore(numeric_limits<streamsize>::max(), '\t');
            getline(in, d, '\t');
            in >> a;
            in.ignore(numeric_limits<streamsize>::max(), '\t');
            getline(in, c);
            id = oldId + 1;
        }
        in.close();

        ofstream out(FILE_NAME, ios::app);
        out << id << '\t' << description << '\t'
            << fixed << setprecision(2) << amount
            << '\t' << category << '\n';
        out.close();

        cout << "{\"success\":true,\"message\":\"Expense added\"}";
    }

    void list() {
        ifstream in(FILE_NAME);
        cout << "{\"transactions\":[";
        bool first = true;
        int id;
        string d, c;
        double a;

        while (in >> id) {
            in.ignore(numeric_limits<streamsize>::max(), '\t');
            getline(in, d, '\t');
            in >> a;
            in.ignore(numeric_limits<streamsize>::max(), '\t');
            getline(in, c);

            if (!first) cout << ",";
            first = false;
            cout << "{\"id\":" << id
                 << ",\"description\":\"" << escapeJson(d)
                 << "\",\"amount\":" << a
                 << ",\"category\":\"" << escapeJson(c) << "\"}";
        }
        cout << "]}";
    }

    void summary() {
        ifstream in(FILE_NAME);
        int id, count = 0;
        string d, c;
        double a, total = 0;

        while (in >> id) {
            in.ignore(numeric_limits<streamsize>::max(), '\t');
            getline(in, d, '\t');
            in >> a;
            in.ignore(numeric_limits<streamsize>::max(), '\t');
            getline(in, c);
            total += a;
            count++;
        }

        cout << "{\"count\":" << count
             << ",\"total\":" << fixed << setprecision(2) << total << "}";
    }
};

int main(int argc, char* argv[]) {
    ExpenseTracker tracker;

    if (argc < 2) {
        cout << "{\"error\":\"Missing command\"}";
        return 1;
    }

    string command = argv[1];

    if (command == "list") {
        tracker.list();
    } else if (command == "summary") {
        tracker.summary();
    } else if (command == "add" && argc == 5) {
        string description = argv[2];
        double amount = atof(argv[3]);
        string category = argv[4];

        if (description.empty() || category.empty() || amount <= 0) {
            cout << "{\"error\":\"Enter a description, category, and positive amount\"}";
            return 1;
        }
        tracker.add(description, amount, category);
    } else {
        cout << "{\"error\":\"Invalid command or arguments\"}";
        return 1;
    }

    return 0;
}
