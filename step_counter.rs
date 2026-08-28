// step_counter.rs
use chrono::Local;
use clap::{App, Arg};
use serde::{Deserialize, Serialize};
use serde_json;
use std::fs;
use std::io::Write;
use colored::*;

const DATA_FILE: &str = "steps.json";
const DEFAULT_GOAL: i32 = 10000;

#[derive(Serialize, Deserialize)]
struct HistoryEntry {
    date: String,
    steps: i32,
}

#[derive(Serialize, Deserialize)]
struct Data {
    goal: i32,
    history: Vec<HistoryEntry>,
}

struct Counter {
    data: Data,
}

impl Counter {
    fn new() -> Self {
        let mut c = Counter { data: Data { goal: DEFAULT_GOAL, history: Vec::new() } };
        c.load();
        c
    }

    fn load(&mut self) {
        if let Ok(json) = fs::read_to_string(DATA_FILE) {
            if let Ok(data) = serde_json::from_str(&json) {
                self.data = data;
                return;
            }
        }
        self.data = Data { goal: DEFAULT_GOAL, history: Vec::new() };
    }

    fn save(&self) {
        let json = serde_json::to_string_pretty(&self.data).unwrap();
        fs::write(DATA_FILE, json).unwrap();
    }

    fn get_today(&self) -> Option<&HistoryEntry> {
        let today = Local::now().format("%Y-%m-%d").to_string();
        self.data.history.iter().find(|e| e.date == today)
    }

    fn get_today_mut(&mut self) -> Option<&mut HistoryEntry> {
        let today = Local::now().format("%Y-%m-%d").to_string();
        self.data.history.iter_mut().find(|e| e.date == today)
    }

    fn add_steps(&mut self, steps: i32) {
        let today = Local::now().format("%Y-%m-%d").to_string();
        if let Some(entry) = self.get_today_mut() {
            entry.steps += steps;
        } else {
            self.data.history.push(HistoryEntry { date: today.clone(), steps });
        }
        self.save();
        let total = self.get_today().map(|e| e.steps).unwrap_or(steps);
        println!("{}", format!("Добавлено {} шагов. Всего сегодня: {}", steps, total).green());
    }

    fn show(&self) {
        let today = Local::now().format("%Y-%m-%d").to_string();
        let steps_today = self.get_today().map(|e| e.steps).unwrap_or(0);
        let progress = (steps_today * 100 / self.data.goal).min(100);
        let bar_len = 20;
        let filled = progress * bar_len / 100;
        let bar = "█".repeat(filled as usize) + "░".repeat((bar_len - filled) as usize);
        println!("{}", format!("📊 Сегодня: {} шагов", steps_today).cyan());
        println!("{}", format!("Цель: {} шагов", self.data.goal).yellow());
        println!("Прогресс: {} {}%", bar.green(), progress);
        println!("{}", "\nИстория за 7 дней:".magenta());
        for i in (0..7).rev() {
            let d = Local::now() - chrono::Duration::days(i);
            let date_str = d.format("%Y-%m-%d").to_string();
            let found = self.data.history.iter().find(|e| e.date == date_str);
            println!("  {}: {}", date_str, found.map(|e| e.steps).unwrap_or(0));
        }
    }

    fn set_goal(&mut self, goal: i32) {
        self.data.goal = goal;
        self.save();
        println!("{}", format!("Цель установлена: {} шагов", goal).green());
    }

    fn history(&self) {
        println!("{}", "История (все записи):".cyan());
        for entry in &self.data.history {
            println!("  {}: {}", entry.date, entry.steps);
        }
    }

    fn reset(&mut self) {
        self.data.history.clear();
        self.save();
        println!("{}", "Все данные сброшены.".green());
    }

    fn export_csv(&self, filename: &str) {
        let mut wtr = csv::Writer::from_path(filename).unwrap();
        wtr.write_record(&["date", "steps"]).unwrap();
        for entry in &self.data.history {
            wtr.write_record(&[&entry.date, &entry.steps.to_string()]).unwrap();
        }
        wtr.flush().unwrap();
        println!("{}", format!("Экспортировано в {} (CSV)", filename).green());
    }
}

fn main() {
    let matches = App::new("Step Counter")
        .arg(Arg::with_name("add").long("add").takes_value(true).help("Добавить шаги за сегодня"))
        .arg(Arg::with_name("show").long("show").help("Показать сегодняшний прогресс и историю"))
        .arg(Arg::with_name("goal").long("goal").takes_value(true).help("Установить дневную цель"))
        .arg(Arg::with_name("history").long("history").help("Показать всю историю"))
        .arg(Arg::with_name("reset").long("reset").help("Сбросить все данные"))
        .arg(Arg::with_name("export-csv").long("export-csv").takes_value(true).help("Экспорт истории в CSV"))
        .get_matches();

    let mut counter = Counter::new();

    if let Some(steps) = matches.value_of("add") {
        let steps: i32 = steps.parse().expect("Неверное число");
        counter.add_steps(steps);
    } else if matches.is_present("show") {
        counter.show();
    } else if let Some(goal) = matches.value_of("goal") {
        let goal: i32 = goal.parse().expect("Неверное число");
        counter.set_goal(goal);
    } else if matches.is_present("history") {
        counter.history();
    } else if matches.is_present("reset") {
        counter.reset();
    } else if let Some(file) = matches.value_of("export-csv") {
        counter.export_csv(file);
    } else {
        println!("Используйте --help для справки.");
    }
}
