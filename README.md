# QuantShoe-Sim: Multithreaded Blackjack Monte Carlo Simulator

A high-performance blackjack simulation engine written in Java. It runs millions of hands across parallel threads to evaluate card-counting strategies (Hi-Lo with Illustrious 18 deviations) and Kelly Criterion bet sizing, measuring expected value, max drawdown, and risk of ruin.

## 📁 Project Structure
* `com.quantshoe` — Entry point and orchestrator (`Main`).
* `com.quantshoe.core` — Card, rank, and shoe modeling (`Card`, `Rank`, `Shoe`).
* `com.quantshoe.engine` — Game logic and hand resolution (`GameEngine`).
* `com.quantshoe.risk` — Kelly bet sizing and count-dependent play deviations (`KellyBettingEngine`, `DeviationMatrix`).
* `com.quantshoe.pipeline` — Multithreaded worker pool and CSV export (`SimulationWorker`, `SimulationResult`, `DataExporter`).
* `com.quantshoe.strategy` — Strategy action definitions (`StrategyAction`).

## ⚡ Design Highlights
* **Zero-GC architecture:** Fixed-size primitive arrays and index pointers instead of heap-allocated collections — no garbage collection pauses during simulation.
* **O(1) strategy lookups:** Playing decisions are pre-compiled into static multidimensional arrays for constant-time access.
* **Count-dependent deviations:** Overrides basic strategy in real time based on the true count (e.g., Illustrious 18 indices).
* **Kelly Criterion bet sizing:** Calculates wager size from estimated edge and bankroll to manage drawdown risk.
* **Thread-parallel simulation:** Uses a `Callable` worker pool to distribute simulation runs across all available CPU cores.

## 🚀 How to Run
1. Clone this repository.
2. Open in IntelliJ IDEA.
3. In `com.quantshoe.Main`, adjust parameters (`simulationRuns`, `handsPerRun`, `startingBankroll`, etc.).
4. Click **Run**. Results print to the console and export to `quant_blackjack_results.csv`.

## 📊 Sample Output
```text
System Hardware Detected: 8 physical cores available.
Allocating Fixed Thread Pool...
Executing 100 parallel tests (10,000,000 cumulative rounds)...
Computation complete! Processing execution time: 1420ms

============== SIMULATION SUMMARY ==============
Total Runs              : 100
Average Net P&L         : +$14,241.50
Worst-Case Max Drawdown : $3,150.00
Risk of Ruin            : 0.00%
=================================================
```
