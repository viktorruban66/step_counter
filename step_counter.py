
#!/usr/bin/env python3
# step_counter.py
import argparse
import json
import os
import sys
from datetime import datetime, timedelta
from colorama import init, Fore, Style

init(autoreset=True)

DATA_FILE = "steps.json"
DEFAULT_GOAL = 10000

class StepCounter:
    def __init__(self):
        self.data = self.load()
        self.goal = self.data.get("goal", DEFAULT_GOAL)
        self.history = self.data.get("history", [])

    def load(self):
        if os.path.exists(DATA_FILE):
            try:
                with open(DATA_FILE, 'r') as f:
                    return json.load(f)
            except:
                pass
        return {"goal": DEFAULT_GOAL, "history": []}

    def save(self):
        with open(DATA_FILE, 'w') as f:
            json.dump({"goal": self.goal, "history": self.history}, f, indent=2)

    def get_today(self):
        today = datetime.now().strftime("%Y-%m-%d")
        for entry in self.history:
            if entry["date"] == today:
                return entry
        return None

    def add_steps(self, steps):
        today = datetime.now().strftime("%Y-%m-%d")
        entry = self.get_today()
        if entry:
            entry["steps"] += steps
        else:
            self.history.append({"date": today, "steps": steps})
        self.save()
        print(Fore.GREEN + f"Добавлено {steps} шагов. Всего сегодня: {entry['steps'] if entry else steps}")

    def show(self):
        today = datetime.now().strftime("%Y-%m-%d")
        entry = self.get_today()
        steps_today = entry["steps"] if entry else 0
        progress = min(100, int((steps_today / self.goal) * 100))
        bar_len = 20
        filled = int(bar_len * progress / 100)
        bar = "█" * filled + "░" * (bar_len - filled)
        print(Fore.CYAN + f"📊 Сегодня: {steps_today} шагов")
        print(Fore.YELLOW + f"Цель: {self.goal} шагов")
        print(f"Прогресс: {Fore.GREEN}{bar} {progress}%")
        # История за 7 дней
        print(Fore.MAGENTA + "\nИстория за 7 дней:")
        today_dt = datetime.now()
        for i in range(6, -1, -1):
            d = (today_dt - timedelta(days=i)).strftime("%Y-%m-%d")
            found = False
            for entry in self.history:
                if entry["date"] == d:
                    print(f"  {d}: {entry['steps']}")
                    found = True
                    break
            if not found:
                print(f"  {d}: 0")

    def set_goal(self, goal):
        self.goal = goal
        self.save()
        print(Fore.GREEN + f"Цель установлена: {goal} шагов")

    def history(self):
        print(Fore.CYAN + "История (все записи):")
        for entry in sorted(self.history, key=lambda x: x["date"]):
            print(f"  {entry['date']}: {entry['steps']}")

    def reset(self):
        self.history = []
        self.save()
        print(Fore.GREEN + "Все данные сброшены.")

    def export_csv(self, filename):
        import csv
        with open(filename, 'w', newline='') as f:
            writer = csv.writer(f)
            writer.writerow(["date", "steps"])
            for entry in self.history:
                writer.writerow([entry["date"], entry["steps"]])
        print(Fore.GREEN + f"Экспортировано в {filename} (CSV)")

def main():
    parser = argparse.ArgumentParser(description="Шагомер (виджет)")
    parser.add_argument("--add", type=int, help="Добавить шаги за сегодня")
    parser.add_argument("--show", action="store_true", help="Показать сегодняшний прогресс и историю")
    parser.add_argument("--goal", type=int, help="Установить дневную цель")
    parser.add_argument("--history", action="store_true", help="Показать всю историю")
    parser.add_argument("--reset", action="store_true", help="Сбросить все данные")
    parser.add_argument("--export-csv", help="Экспорт истории в CSV")
    args = parser.parse_args()

    counter = StepCounter()
    if args.add:
        counter.add_steps(args.add)
    elif args.show:
        counter.show()
    elif args.goal:
        counter.set_goal(args.goal)
    elif args.history:
        counter.history()
    elif args.reset:
        counter.reset()
    elif args.export_csv:
        counter.export_csv(args.export_csv)
    else:
        parser.print_help()

if __name__ == "__main__":
    main()
