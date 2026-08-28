// StepCounter.cs
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace StepCounter
{
    class Program
    {
        static void Main(string[] args)
        {
            var opts = ParseArgs(args);
            var counter = new StepCounter();
            if (opts.Add.HasValue) counter.AddSteps(opts.Add.Value);
            else if (opts.Show) counter.Show();
            else if (opts.Goal.HasValue) counter.SetGoal(opts.Goal.Value);
            else if (opts.History) counter.History();
            else if (opts.Reset) counter.Reset();
            else if (opts.ExportCsv != null) counter.ExportCsv(opts.ExportCsv);
            else Console.WriteLine("Используйте --help для справки.");
        }

        static Options ParseArgs(string[] args)
        {
            var opts = new Options();
            for (int i = 0; i < args.Length; i++)
            {
                switch (args[i])
                {
                    case "--add": opts.Add = int.Parse(args[++i]); break;
                    case "--show": opts.Show = true; break;
                    case "--goal": opts.Goal = int.Parse(args[++i]); break;
                    case "--history": opts.History = true; break;
                    case "--reset": opts.Reset = true; break;
                    case "--export-csv": opts.ExportCsv = args[++i]; break;
                }
            }
            return opts;
        }

        class Options
        {
            public int? Add { get; set; }
            public bool Show { get; set; }
            public int? Goal { get; set; }
            public bool History { get; set; }
            public bool Reset { get; set; }
            public string ExportCsv { get; set; }
        }

        class HistoryEntry
        {
            public string Date { get; set; }
            public int Steps { get; set; }
        }

        class Data
        {
            public int Goal { get; set; } = 10000;
            public List<HistoryEntry> History { get; set; } = new List<HistoryEntry>();
        }

        class StepCounter
        {
            private const string DataFile = "steps.json";
            private Data data;

            public StepCounter()
            {
                Load();
            }

            private void Load()
            {
                try
                {
                    if (File.Exists(DataFile))
                    {
                        string json = File.ReadAllText(DataFile);
                        data = JsonSerializer.Deserialize<Data>(json) ?? new Data();
                    }
                    else
                        data = new Data();
                }
                catch
                {
                    data = new Data();
                }
                if (data.History == null) data.History = new List<HistoryEntry>();
            }

            private void Save()
            {
                string json = JsonSerializer.Serialize(data, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(DataFile, json);
            }

            private HistoryEntry GetToday()
            {
                string today = DateTime.UtcNow.ToString("yyyy-MM-dd");
                return data.History.FirstOrDefault(e => e.Date == today);
            }

            public void AddSteps(int steps)
            {
                var entry = GetToday();
                if (entry != null)
                    entry.Steps += steps;
                else
                    data.History.Add(new HistoryEntry { Date = DateTime.UtcNow.ToString("yyyy-MM-dd"), Steps = steps });
                Save();
                int total = entry != null ? entry.Steps : steps;
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Добавлено {steps} шагов. Всего сегодня: {total}");
                Console.ResetColor();
            }

            public void Show()
            {
                var entry = GetToday();
                int stepsToday = entry != null ? entry.Steps : 0;
                int progress = Math.Min(100, stepsToday * 100 / data.Goal);
                int barLen = 20;
                int filled = progress * barLen / 100;
                string bar = new string('█', filled) + new string('░', barLen - filled);
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine($"📊 Сегодня: {stepsToday} шагов");
                Console.ForegroundColor = ConsoleColor.Yellow;
                Console.WriteLine($"Цель: {data.Goal} шагов");
                Console.ResetColor();
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Прогресс: {bar} {progress}%");
                Console.ResetColor();
                Console.ForegroundColor = ConsoleColor.Magenta;
                Console.WriteLine("\nИстория за 7 дней:");
                Console.ResetColor();
                for (int i = 6; i >= 0; i--)
                {
                    var d = DateTime.UtcNow.AddDays(-i);
                    string dateStr = d.ToString("yyyy-MM-dd");
                    var found = data.History.FirstOrDefault(e => e.Date == dateStr);
                    Console.WriteLine($"  {dateStr}: {(found != null ? found.Steps : 0)}");
                }
            }

            public void SetGoal(int goal)
            {
                data.Goal = goal;
                Save();
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Цель установлена: {goal} шагов");
                Console.ResetColor();
            }

            public void History()
            {
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine("История (все записи):");
                Console.ResetColor();
                foreach (var e in data.History.OrderBy(e => e.Date))
                    Console.WriteLine($"  {e.Date}: {e.Steps}");
            }

            public void Reset()
            {
                data.History.Clear();
                Save();
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine("Все данные сброшены.");
                Console.ResetColor();
            }

            public void ExportCsv(string filename)
            {
                using var sw = new StreamWriter(filename);
                sw.WriteLine("date,steps");
                foreach (var e in data.History)
                    sw.WriteLine($"{e.Date},{e.Steps}");
                Console.ForegroundColor = ConsoleColor.Green;
                Console.WriteLine($"Экспортировано в {filename} (CSV)");
                Console.ResetColor();
            }
        }
    }
}
