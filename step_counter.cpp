// step_counter.cpp
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <algorithm>
#include <ctime>
#include <iomanip>
#include <sstream>
#include <json/json.h> // using jsoncpp

using namespace std;

const string DATA_FILE = "steps.json";
const int DEFAULT_GOAL = 10000;

struct HistoryEntry {
    string date;
    int steps;
};

struct Data {
    int goal = DEFAULT_GOAL;
    vector<HistoryEntry> history;
};

class Counter {
private:
    Data data;

    void load() {
        ifstream ifs(DATA_FILE);
        if (!ifs) return;
        Json::Value root;
        ifs >> root;
        data.goal = root.get("goal", DEFAULT_GOAL).asInt();
        for (const auto& item : root["history"]) {
            HistoryEntry e;
            e.date = item["date"].asString();
            e.steps = item["steps"].asInt();
            data.history.push_back(e);
        }
    }

    void save() {
        Json::Value root;
        root["goal"] = data.goal;
        for (const auto& e : data.history) {
            Json::Value item;
            item["date"] = e.date;
            item["steps"] = e.steps;
            root["history"].append(item);
        }
        ofstream ofs(DATA_FILE);
        ofs << root.toStyledString();
    }

    string today() {
        time_t t = time(nullptr);
        tm* now = localtime(&t);
        char buf[11];
        strftime(buf, sizeof(buf), "%Y-%m-%d", now);
        return string(buf);
    }

    HistoryEntry* getToday() {
        string td = today();
        for (auto& e : data.history) {
            if (e.date == td) return &e;
        }
        return nullptr;
    }

public:
    Counter() { load(); }

    void addSteps(int steps) {
        auto entry = getToday();
        if (entry) {
            entry->steps += steps;
        } else {
            HistoryEntry e;
            e.date = today();
            e.steps = steps;
            data.history.push_back(e);
        }
        save();
        int total = entry ? entry->steps : steps;
        cout << "\033[32mДобавлено " << steps << " шагов. Всего сегодня: " << total << "\033[0m" << endl;
    }

    void show() {
        auto entry = getToday();
        int stepsToday = entry ? entry->steps : 0;
        int progress = stepsToday * 100 / data.goal;
        if (progress > 100) progress = 100;
        int barLen = 20;
        int filled = progress * barLen / 100;
        string bar(filled, '█');
        bar.append(barLen - filled, '░');
        cout << "\033[36m📊 Сегодня: " << stepsToday << " шагов\033[0m" << endl;
        cout << "\033[33mЦель: " << data.goal << " шагов\033[0m" << endl;
        cout << "Прогресс: \033[32m" << bar << "\033[0m " << progress << "%" << endl;
        cout << "\033[35mИстория за 7 дней:\033[0m" << endl;
        time_t t = time(nullptr);
        tm* now = localtime(&t);
        for (int i = 6; i >= 0; --i) {
            tm d = *now;
            d.tm_mday -= i;
            mktime(&d);
            char buf[11];
            strftime(buf, sizeof(buf), "%Y-%m-%d", &d);
            string dateStr(buf);
            int steps = 0;
            for (const auto& e : data.history) {
                if (e.date == dateStr) { steps = e.steps; break; }
            }
            cout << "  " << dateStr << ": " << steps << endl;
        }
    }

    void setGoal(int goal) {
        data.goal = goal;
        save();
        cout << "\033[32mЦель установлена: " << goal << " шагов\033[0m" << endl;
    }

    void history() {
        cout << "\033[36mИстория (все записи):\033[0m" << endl;
        sort(data.history.begin(), data.history.end(), [](const HistoryEntry& a, const HistoryEntry& b) {
            return a.date < b.date;
        });
        for (const auto& e : data.history) {
            cout << "  " << e.date << ": " << e.steps << endl;
        }
    }

    void reset() {
        data.history.clear();
        save();
        cout << "\033[32mВсе данные сброшены.\033[0m" << endl;
    }

    void exportCSV(const string& filename) {
        ofstream ofs(filename);
        ofs << "date,steps\n";
        for (const auto& e : data.history) {
            ofs << e.date << "," << e.steps << "\n";
        }
        cout << "\033[32mЭкспортировано в " << filename << " (CSV)\033[0m" << endl;
    }
};

int main(int argc, char* argv[]) {
    int add = 0, goal = 0;
    bool show = false, history = false, reset = false;
    string exportCsv;

    for (int i = 1; i < argc; ++i) {
        string arg = argv[i];
        if (arg == "--add" && i+1 < argc) add = stoi(argv[++i]);
        else if (arg == "--show") show = true;
        else if (arg == "--goal" && i+1 < argc) goal = stoi(argv[++i]);
        else if (arg == "--history") history = true;
        else if (arg == "--reset") reset = true;
        else if (arg == "--export-csv" && i+1 < argc) exportCsv = argv[++i];
    }

    Counter counter;
    if (add > 0) counter.addSteps(add);
    else if (show) counter.show();
    else if (goal > 0) counter.setGoal(goal);
    else if (history) counter.history();
    else if (reset) counter.reset();
    else if (!exportCsv.empty()) counter.exportCSV(exportCsv);
    else cout << "Используйте --help для справки." << endl;
    return 0;
}
