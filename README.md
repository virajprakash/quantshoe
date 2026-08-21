# QuantShoe-Sim: Multithreaded Blackjack Monte Carlo Simulator

A high-performance blackjack simulation engine written in Java. It runs millions of hands across parallel threads to evaluate Hi-Lo card-counting strategies under realistic casino conditions, measuring expected value, max drawdown, and risk of ruin.

## 📁 Project Structure
* `com.quantshoe` — Entry point and simulation orchestrator (`Main`).
* `com.quantshoe.core` — Card, rank, and shoe modeling with Hi-Lo running/true count tracking (`Card`, `Rank`, `Shoe`).
* `com.quantshoe.engine` — Game logic and hand resolution for H17 and S17 rule sets (`GameEngine`, `S17GameEngine`).
* `com.quantshoe.risk` — Bet sizing strategies and count-dependent play deviations (`KellyBettingEngine`, `FixedSpreadBettingEngine`, `DeviationMatrix`).
* `com.quantshoe.pipeline` — Multithreaded worker pool, result aggregation, and CSV export (`SimulationWorker`, `SimulationResult`, `DataExporter`).
* `com.quantshoe.strategy` — Strategy action definitions including deviation-aware actions (`StrategyAction`).

## ⚡ Design Highlights
* **Constant-time strategy lookups:** Playing decisions are pre-compiled into static multidimensional arrays, giving O(1) access for every hand/dealer combination.
* **Count-based playing deviations:** Overrides basic strategy in real time based on the true count, including surrender index plays (e.g., surrender 16 vs 9 above −1, stand 15 vs 10 above +4).
* **Two betting modes:**
  * **Half-Kelly Criterion** — sizes wagers from estimated edge and current bankroll to balance growth against drawdown risk.
  * **Fixed spread (CVCX-matched)** — ramps bets from table minimum to maximum on a 1-2-6-10-12 unit schedule keyed to the true count.
* **Multi-hand play:** At high true counts the fixed spread engine automatically spreads to two hands to increase hourly EV.
* **Configurable rule sets:** Supports H17/S17 dealer rules, late surrender, resplit aces, and adjustable deck penetration.
* **Thread-parallel simulation:** Uses a fixed-size `ExecutorService` thread pool to distribute simulation runs across all available CPU cores.

## 🚀 How to Run
1. Clone this repository.
2. Open in IntelliJ IDEA (JUnit 5 standalone JAR is included under `lib/`).
3. Build and run with optional command-line arguments to override defaults:
   ```
   java com.quantshoe.Main [options]
   ```
   | Flag | Type | Default | Description |
   |------|------|---------|-------------|
   | `--simRuns` | int | 1000 | Number of simulation runs |
   | `--handsPerRun` | int | 100000 | Hands per run |
   | `--startingBankroll` | double | 10000 | Starting bankroll |
   | `--tableMin` | double | 25 | Table minimum bet |
   | `--tableMax` | double | 3000 | Table maximum bet |
   | `--deckPen` | double | 1.5 | Deck penetration (decks cut off before reshuffle) |
   | `--gameMode` | string | S17 | Dealer soft-17 rule (`S17` or `H17`) |
   | `--bettingMode` | string | fixed | Bet sizing strategy (`kelly` or `fixed`) |

   **Example:**
   ```bash
   java com.quantshoe.Main --simRuns 500 --handsPerRun 200000 --startingBankroll 20000 --tableMin 50 --tableMax 5000 --deckPen 1.0 --gameMode H17 --bettingMode kelly
   ```
   All flags are optional — omitted flags use their default values.
4. Results print to the console and export to `quant_blackjack_results.csv`.
5. *(Optional)* Run `python visualize_results.py` to generate plots from the CSV output.

## 📊 Sample Output
```text
System Hardware Detected: 8 physical cores available.
Allocating Fixed Thread Pool...
Betting Mode: Fixed Spread ($25-$3000)
Game Engine: S17 (Stand on Soft 17)
Executing 1000 parallel tests (100,000,000 cumulative rounds)...
Computation complete! Processing execution time: 1420ms

============== SIMULATION SUMMARY ==============
Total Runs              : 1000
Average Net P&L         : $100912.71
Worst-Case Max Drawdown : $49987.50
Risk of Ruin            : 6.40%
=================================================
```

The console also prints an **EV-by-rounds-per-hour table** (hourly and 8-hour session projections) and the **full bet spread** for every true count from −5 to +10.
```
========== EXPECTED VALUE BY ROUNDS PER HOUR ==========
Rounds/Hour          EV/Hour            EV/8hr Session     Hours/Thread       8hr Sessions/Thread
-----------------------------------------------------------------------------------------------
50                   $47.59             $380.70            2120.6             265.1             
60                   $57.11             $456.84            1767.1             220.9             
70                   $66.62             $532.98            1514.7             189.3             
80                   $76.14             $609.12            1325.3             165.7             
100                  $95.18             $761.41            1060.3             132.5             
120                  $114.21            $913.69            883.6              110.4             
150                  $142.76            $1142.11           706.9              88.4              
200                  $190.35            $1522.81           530.1              66.3              
===============================================================================================

=================== BET SPREAD (Starting Bankroll) ===================
True Count         Wager             
--------------------------------------
-5                 $25 x 1           
-4                 $25 x 1           
-3                 $25 x 1           
-2                 $25 x 1           
-1                 $25 x 1           
+0                 $25 x 1           
+1                 $25 x 1           
+2                 $50 x 2           
+3                 $150 x 2          
+4                 $250 x 2          
+5                 $300 x 2          
+6                 $300 x 2          
+7                 $300 x 2          
+8                 $300 x 2          
+9                 $300 x 2          
+10                $300 x 2                  
======================================================================
```