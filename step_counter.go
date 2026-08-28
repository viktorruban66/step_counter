// step_counter.go
package main

import (
	"encoding/csv"
	"encoding/json"
	"flag"
	"fmt"
	"os"
	"sort"
	"strconv"
	"time"
)

const dataFile = "steps.json"
const defaultGoal = 10000

type HistoryEntry struct {
	Date  string `json:"date"`
	Steps int    `json:"steps"`
}

type Data struct {
	Goal    int            `json:"goal"`
	History []HistoryEntry `json:"history"`
}

type Counter struct {
	data Data
}

func (c *Counter) load() {
	file, err := os.ReadFile(dataFile)
	if err != nil {
		c.data = Data{Goal: defaultGoal, History: []HistoryEntry{}}
		return
	}
	if err := json.Unmarshal(file, &c.data); err != nil {
		c.data = Data{Goal: defaultGoal, History: []HistoryEntry{}}
	}
}

func (c *Counter) save() {
	data, _ := json.MarshalIndent(c.data, "", "  ")
	os.WriteFile(dataFile, data, 0644)
}

func (c *Counter) getToday() *HistoryEntry {
	today := time.Now().Format("2006-01-02")
	for i := range c.data.History {
		if c.data.History[i].Date == today {
			return &c.data.History[i]
		}
	}
	return nil
}

func (c *Counter) addSteps(steps int) {
	today := time.Now().Format("2006-01-02")
	entry := c.getToday()
	if entry != nil {
		entry.Steps += steps
	} else {
		c.data.History = append(c.data.History, HistoryEntry{Date: today, Steps: steps})
	}
	c.save()
	total := 0
	if entry != nil {
		total = entry.Steps
	} else {
		total = steps
	}
	fmt.Printf("\033[32mДобавлено %d шагов. Всего сегодня: %d\033[0m\n", steps, total)
}

func (c *Counter) show() {
	today := time.Now().Format("2006-01-02")
	entry := c.getToday()
	stepsToday := 0
	if entry != nil {
		stepsToday = entry.Steps
	}
	progress := stepsToday * 100 / c.data.Goal
	if progress > 100 {
		progress = 100
	}
	barLen := 20
	filled := progress * barLen / 100
	bar := ""
	for i := 0; i < filled; i++ {
		bar += "█"
	}
	for i := filled; i < barLen; i++ {
		bar += "░"
	}
	fmt.Printf("\033[36m📊 Сегодня: %d шагов\033[0m\n", stepsToday)
	fmt.Printf("\033[33mЦель: %d шагов\033[0m\n", c.data.Goal)
	fmt.Printf("Прогресс: \033[32m%s\033[0m %d%%\n", bar, progress)
	fmt.Println("\033[35mИстория за 7 дней:\033[0m")
	for i := 6; i >= 0; i-- {
		d := time.Now().AddDate(0, 0, -i).Format("2006-01-02")
		found := false
		for _, e := range c.data.History {
			if e.Date == d {
				fmt.Printf("  %s: %d\n", d, e.Steps)
				found = true
				break
			}
		}
		if !found {
			fmt.Printf("  %s: 0\n", d)
		}
	}
}

func (c *Counter) setGoal(goal int) {
	c.data.Goal = goal
	c.save()
	fmt.Printf("\033[32mЦель установлена: %d шагов\033[0m\n", goal)
}

func (c *Counter) history() {
	fmt.Println("\033[36mИстория (все записи):\033[0m")
	sort.Slice(c.data.History, func(i, j int) bool {
		return c.data.History[i].Date < c.data.History[j].Date
	})
	for _, e := range c.data.History {
		fmt.Printf("  %s: %d\n", e.Date, e.Steps)
	}
}

func (c *Counter) reset() {
	c.data.History = []HistoryEntry{}
	c.save()
	fmt.Println("\033[32mВсе данные сброшены.\033[0m")
}

func (c *Counter) exportCSV(filename string) {
	file, err := os.Create(filename)
	if err != nil {
		fmt.Printf("Ошибка создания файла: %v\n", err)
		return
	}
	defer file.Close()
	w := csv.NewWriter(file)
	defer w.Flush()
	w.Write([]string{"date", "steps"})
	for _, e := range c.data.History {
		w.Write([]string{e.Date, strconv.Itoa(e.Steps)})
	}
	fmt.Printf("\033[32mЭкспортировано в %s (CSV)\033[0m\n", filename)
}

func main() {
	var (
		add      int
		show     bool
		goal     int
		history  bool
		reset    bool
		exportCSV string
	)
	flag.IntVar(&add, "add", 0, "Добавить шаги за сегодня")
	flag.BoolVar(&show, "show", false, "Показать сегодняшний прогресс и историю")
	flag.IntVar(&goal, "goal", 0, "Установить дневную цель")
	flag.BoolVar(&history, "history", false, "Показать всю историю")
	flag.BoolVar(&reset, "reset", false, "Сбросить все данные")
	flag.StringVar(&exportCSV, "export-csv", "", "Экспорт истории в CSV")
	flag.Parse()

	counter := &Counter{}
	counter.load()

	if add > 0 {
		counter.addSteps(add)
	} else if show {
		counter.show()
	} else if goal > 0 {
		counter.setGoal(goal)
	} else if history {
		counter.history()
	} else if reset {
		counter.reset()
	} else if exportCSV != "" {
		counter.exportCSV(exportCSV)
	} else {
		fmt.Println("Используйте --help для справки.")
	}
}
