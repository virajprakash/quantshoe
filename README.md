# QuantShoe-Sim: Multi-Threaded O(1) Risk & Variance Simulation Engine

A high-performance, memory-flat quantitative backtesting architecture written natively in Java. This asset engine executes millions of concurrent probability paths to audit structural variance, expected value (EV), and maximum drawdown (MDD) constraints using fractional Kelly Criterion resource allocation models and real-time count-dependent index deviations.

## 📁 Project Architecture
The codebase is decoupled into modular layers following standard enterprise design patterns:
* `com.quantshoe` - Application entry point and orchestrator (`Main`).
* `com.quantshoe.core` - Domain layer handling physics entities (`Card`, `Rank`, `BlackjackShoe`).
* `com.quantshoe.engine` - Execution layer automating high-speed rule logic (`GameEngine`).
* `com.quantshoe.risk` - Quant layer implementing financial math formulas (`KellyBettingEngine`, `DeviationMatrix`).
* `com.quantshoe.pipeline` - Concurrency pipeline handling worker threads and CSV output streams (`SimulationWorker`, `SimulationResult`, `DataExporter`).
* `com.quantshoe.strategy` - Strategy policy layers (`StrategyAction`).

## ⚡ Key Engineering & Quant Enhancements
* **Zero Garbage Collection Footprint:** Uses fixed-size primitive tracking buffers and index pointers instead of dynamic heap allocations to ensure zero runtime latency spikes.
* **O(1) Matrix Lookup Performance:** Pre-compiles tactical strategy decision tables into multidimensional static memory spaces for immediate O(1) array routing.
* **Dynamic Index Deviations:** Intercepts baseline playing rules in real time to apply count-dependent adjustments (e.g., Illustrious 18) based on current True Count volatility vectors.
* **Adaptive Kelly Allocation:** Integrates asset probability edge metrics with custom fractional Kelly sizing calculations to smooth drawdown profiles.
* **Core Parallelism Scaling:** Utilizes an isolated, thread-safe asynchronous `Callable` worker pool to scale computations linearly across host CPU threads.

## 🚀 How to Run the Benchmarks
1. Clone this repository to your local machine.
2. Open the project folder inside IntelliJ IDEA.
3. Open `com.quantshoe.Main` and adjust your target test parameters (`simulationRuns`, `handsPerRun`, `startingBankroll`).
4. Click **Run**. The engine will execute the parallel tracks, output a detailed risk summary report to the console, and generate a spreadsheet-ready dataset (`quant_blackjack_results.csv`).

## 📊 Sample Performance Summary Output
```text
System Hardware Detected: 8 physical cores available.
Allocating Fixed Thread Pool...
Executing 100 parallel tests (10,000,000 cumulative rounds)...
Computation complete! Processing execution time: 1420ms

============== RISK AUDIT SYSTEM EXECUTIVE REPORT ==============
Total Simulated Timelines : 100
Average Expected Net P&L  : +\$14,241.50
Worst-Case Absolute MDD   : \$3,150.00
Calculated Risk of Ruin   : 0.00%
================================================================
```
