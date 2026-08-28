#!/usr/bin/env node
// step_counter.js
const { program } = require('commander');
const fs = require('fs');
const chalk = require('chalk');

const DATA_FILE = 'steps.json';
const DEFAULT_GOAL = 10000;

class StepCounter {
    constructor() {
        this.data = this.load();
        this.goal = this.data.goal || DEFAULT_GOAL;
        this.history = this.data.history || [];
    }

    load() {
        try {
            if (fs.existsSync(DATA_FILE)) {
                return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
            }
        } catch (e) {}
        return { goal: DEFAULT_GOAL, history: [] };
    }

    save() {
        fs.writeFileSync(DATA_FILE, JSON.stringify({ goal: this.goal, history: this.history }, null, 2));
    }

    getToday() {
        const today = new Date().toISOString().split('T')[0];
        return this.history.find(e => e.date === today);
    }

    addSteps(steps) {
        const today = new Date().toISOString().split('T')[0];
        const entry = this.getToday();
        if (entry) {
            entry.steps += steps;
        } else {
            this.history.push({ date: today, steps });
        }
        this.save();
        const total = entry ? entry.steps : steps;
        console.log(chalk.green(`Добавлено ${steps} шагов. Всего сегодня: ${total}`));
    }

    show() {
        const today = new Date().toISOString().split('T')[0];
        const entry = this.getToday();
        const stepsToday = entry ? entry.steps : 0;
        const progress = Math.min(100, Math.floor((stepsToday / this.goal) * 100));
        const barLen = 20;
        const filled = Math.floor(barLen * progress / 100);
        const bar = '█'.repeat(filled) + '░'.repeat(barLen - filled);
        console.log(chalk.cyan(`📊 Сегодня: ${stepsToday} шагов`));
        console.log(chalk.yellow(`Цель: ${this.goal} шагов`));
        console.log(`Прогресс: ${chalk.green(bar)} ${progress}%`);
        console.log(chalk.magenta('\nИстория за 7 дней:'));
        const todayDt = new Date();
        for (let i = 6; i >= 0; i--) {
            const d = new Date(todayDt);
            d.setDate(d.getDate() - i);
            const dateStr = d.toISOString().split('T')[0];
            const found = this.history.find(e => e.date === dateStr);
            console.log(`  ${dateStr}: ${found ? found.steps : 0}`);
        }
    }

    setGoal(goal) {
        this.goal = goal;
        this.save();
        console.log(chalk.green(`Цель установлена: ${goal} шагов`));
    }

    history() {
        console.log(chalk.cyan('История (все записи):'));
        this.history.sort((a, b) => a.date.localeCompare(b.date));
        for (const e of this.history) {
            console.log(`  ${e.date}: ${e.steps}`);
        }
    }

    reset() {
        this.history = [];
        this.save();
        console.log(chalk.green('Все данные сброшены.'));
    }

    exportCsv(filename) {
        const header = 'date,steps\n';
        const rows = this.history.map(e => `${e.date},${e.steps}`).join('\n');
        fs.writeFileSync(filename, header + rows);
        console.log(chalk.green(`Экспортировано в ${filename} (CSV)`));
    }
}

program
    .option('--add <steps>', 'Добавить шаги за сегодня', parseInt)
    .option('--show', 'Показать сегодняшний прогресс и историю')
    .option('--goal <steps>', 'Установить дневную цель', parseInt)
    .option('--history', 'Показать всю историю')
    .option('--reset', 'Сбросить все данные')
    .option('--export-csv <file>', 'Экспорт истории в CSV')
    .parse(process.argv);

const opts = program.opts();
const counter = new StepCounter();

if (opts.add) counter.addSteps(opts.add);
else if (opts.show) counter.show();
else if (opts.goal) counter.setGoal(opts.goal);
else if (opts.history) counter.history();
else if (opts.reset) counter.reset();
else if (opts.exportCsv) counter.exportCsv(opts.exportCsv);
else program.help();
