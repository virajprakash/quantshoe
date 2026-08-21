import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.ticker as mticker
import numpy as np

# Load data
df = pd.read_csv('quant_blackjack_results.csv')

# Derived columns
starting_bankroll = 25000  # infer from context
df['ROI_pct'] = (df['Net_Profit'] / starting_bankroll) * 100
df['Busted'] = df['Final_Bankroll'] <= 25  # approximate table min
df['Drawdown_pct'] = (df['Max_Drawdown'] / (starting_bankroll + df['Net_Profit'].clip(lower=0))) * 100

def dollar_formatter(x, pos):
    """Format axis ticks as readable dollar amounts (e.g., $50K, $1.2M)."""
    if abs(x) >= 1_000_000:
        return f'${x/1_000_000:.1f}M'
    elif abs(x) >= 1_000:
        return f'${x/1_000:.0f}K'
    else:
        return f'${x:.0f}'

# Simulation starting conditions
num_decks = 6
hands_per_run = 100_000
betting_mode = 'Fixed Spread (1-12)'
table_min = 25
table_max = 3000

# Game rules
game_rules = [
    'Dealer Hits on Soft 17 (H17)',
    'Double After Split Allowed (DAS)',
    'Late Surrender Allowed',
    'Blackjack Pays 3:2',
    'Insurance Offered (Ace Up)',
    'Re-split Aces Not Allowed',
    'No Draw to Splitted Aces',
    'Penetration: 1.5',
    'Deck Estimation: Full'
]

# Use a clean style
plt.style.use('seaborn-v0_8-darkgrid')
fig = plt.figure(figsize=(20, 14))

# Create grid: top row for conditions banner, bottom 2 rows for charts
gs = fig.add_gridspec(3, 3, height_ratios=[0.45, 1, 1], hspace=0.35, wspace=0.3)
fig.suptitle('Blackjack Monte Carlo Simulation — Analysis', fontsize=16, fontweight='bold', y=0.98)

# --- Starting Conditions Banner (top row, spanning all columns) ---
ax_cond = fig.add_subplot(gs[0, :])
ax_cond.axis('off')

conditions_left = (
    f"  {'SIMULATION PARAMETERS':^40}\n"
    f"  {'─' * 40}\n"
    f"  Simulation Runs:      {len(df):,}\n"
    f"  Hands per Run:        {hands_per_run:,}\n"
    f"  Starting Bankroll:    ${starting_bankroll:,}\n"
    f"  Table Minimum:        ${table_min:,}\n"
    f"  Table Maximum:        ${table_max:,}\n"
    f"  Betting Strategy:     {betting_mode}\n"
    f"  Shoe Size:            {num_decks} decks"
)

conditions_right = (
    f"  {'GAME RULES':^40}\n"
    f"  {'─' * 40}\n"
    + '\n'.join(f'  • {rule}' for rule in game_rules)
)

ax_cond.text(0.18, 0.5, conditions_left, transform=ax_cond.transAxes, fontsize=11,
             verticalalignment='center', fontfamily='monospace',
             bbox=dict(boxstyle='round,pad=0.8', facecolor='#e3f2fd', edgecolor='#1565C0', alpha=0.9))
ax_cond.text(0.58, 0.5, conditions_right, transform=ax_cond.transAxes, fontsize=11,
             verticalalignment='center', fontfamily='monospace',
             bbox=dict(boxstyle='round,pad=0.8', facecolor='#e8f5e9', edgecolor='#2E7D32', alpha=0.9))

# Create chart axes from the grid
axes = [[fig.add_subplot(gs[r, c]) for c in range(3)] for r in range(1, 3)]

# 1. Net Profit Distribution
ax = axes[0][0]
profits = df['Net_Profit']
ax.hist(profits, bins=60, color='#2196F3', edgecolor='black', alpha=0.8)
ax.axvline(profits.mean(), color='red', linestyle='--', linewidth=2, label=f'Mean: ${profits.mean():,.0f}')
ax.axvline(profits.median(), color='orange', linestyle='--', linewidth=2, label=f'Median: ${profits.median():,.0f}')
ax.axvline(0, color='black', linestyle='-', linewidth=1.5)
ax.set_title('Net Profit Distribution', fontsize=13, fontweight='bold')
ax.set_xlabel('Net Profit ($)')
ax.set_ylabel('Frequency')
ax.xaxis.set_major_formatter(mticker.FuncFormatter(dollar_formatter))
ax.xaxis.get_offset_text().set_visible(False)
ax.legend(fontsize=9)

# 2. Final Bankroll Distribution (log scale for spread)
ax = axes[0][1]
non_bust = df[~df['Busted']]['Final_Bankroll']
bust_count = df['Busted'].sum()
ax.hist(non_bust, bins=50, color='#4CAF50', edgecolor='black', alpha=0.8)
ax.set_title(f'Final Bankroll Distribution\n({bust_count} busted runs excluded)', fontsize=13, fontweight='bold')
ax.set_xlabel('Final Bankroll ($)')
ax.set_ylabel('Frequency')
ax.axvline(non_bust.mean(), color='red', linestyle='--', linewidth=2, label=f'Mean: ${non_bust.mean():,.0f}')
ax.xaxis.set_major_formatter(mticker.FuncFormatter(dollar_formatter))
ax.xaxis.get_offset_text().set_visible(False)
ax.legend(fontsize=9)

# 3. Max Drawdown Distribution
ax = axes[0][2]
ax.hist(df['Max_Drawdown'], bins=50, color='#FF5722', edgecolor='black', alpha=0.8)
ax.axvline(df['Max_Drawdown'].mean(), color='blue', linestyle='--', linewidth=2, label=f'Mean: ${df["Max_Drawdown"].mean():,.0f}')
ax.axvline(df['Max_Drawdown'].median(), color='cyan', linestyle='--', linewidth=2, label=f'Median: ${df["Max_Drawdown"].median():,.0f}')
ax.set_title('Max Drawdown Distribution', fontsize=13, fontweight='bold')
ax.set_xlabel('Max Drawdown ($)')
ax.set_ylabel('Frequency')
ax.xaxis.set_major_formatter(mticker.FuncFormatter(dollar_formatter))
ax.legend(fontsize=9)

# 4. Win/Loss/Bust Pie Chart
ax = axes[1][0]
winners = (df['Net_Profit'] > 0).sum()
losers = ((df['Net_Profit'] <= 0) & (~df['Busted'])).sum()
busted = df['Busted'].sum()
sizes = [winners, losers, busted]
labels = [f'Profitable\n({winners})', f'Loss (survived)\n({losers})', f'Busted\n({busted})']
colors = ['#4CAF50', '#FFC107', '#F44336']
explode = (0.03, 0.03, 0.08)
ax.pie(sizes, labels=labels, colors=colors, explode=explode, autopct='%1.1f%%',
       shadow=True, startangle=90, textprops={'fontsize': 10})
ax.set_title('Run Outcomes', fontsize=13, fontweight='bold')

# 5. Profit vs Max Drawdown Scatter
ax = axes[1][1]
colors_scatter = ['#F44336' if b else '#2196F3' for b in df['Busted']]
ax.scatter(df['Max_Drawdown'], df['Net_Profit'], c=colors_scatter, alpha=0.4, s=15, edgecolors='none')
ax.axhline(0, color='black', linestyle='-', linewidth=1)
ax.set_title('Net Profit vs Max Drawdown', fontsize=13, fontweight='bold')
ax.set_xlabel('Max Drawdown ($)')
ax.set_ylabel('Net Profit ($)')
ax.xaxis.set_major_formatter(mticker.FuncFormatter(dollar_formatter))
ax.yaxis.set_major_formatter(mticker.FuncFormatter(dollar_formatter))
ax.xaxis.get_offset_text().set_visible(False)
ax.yaxis.get_offset_text().set_visible(False)
# Add legend
from matplotlib.lines import Line2D
legend_elements = [Line2D([0], [0], marker='o', color='w', markerfacecolor='#2196F3', markersize=8, label='Survived'),
                   Line2D([0], [0], marker='o', color='w', markerfacecolor='#F44336', markersize=8, label='Busted')]
ax.legend(handles=legend_elements, fontsize=9)

# 6. Key Statistics Text Box
ax = axes[1][2]
ax.axis('off')
total_runs = len(df)
avg_profit = profits.mean()
median_profit = profits.median()
std_profit = profits.std()
ror = (busted / total_runs) * 100
avg_drawdown = df['Max_Drawdown'].mean()
max_drawdown = df['Max_Drawdown'].max()
avg_hands = df['Total_Hands_Played'].mean()
best_run = profits.max()
worst_run = profits.min()
pct_profitable = (winners / total_runs) * 100

# EV per hand and per hour
ev_per_hand = profits.sum() / df['Total_Hands_Played'].sum()
ev_150rph = ev_per_hand * 150

stats_text = (
    f"{'═' * 42}\n"
    f"  KEY PERFORMANCE METRICS\n"
    f"{'═' * 42}\n\n"
    f"  Total Simulation Runs:    {total_runs:,}\n"
    f"  Avg Hands/Run:            {avg_hands:,.0f}\n\n"
    f"  Average Net Profit:       ${avg_profit:,.0f}\n"
    f"  Median Net Profit:        ${median_profit:,.0f}\n"
    f"  Std Dev of Profit:        ${std_profit:,.0f}\n\n"
    f"  Best Run:                 ${best_run:,.0f}\n"
    f"  Worst Run:                ${worst_run:,.0f}\n\n"
    f"  % Profitable Runs:        {pct_profitable:.1f}%\n"
    f"  Risk of Ruin:             {ror:.1f}%\n\n"
    f"  Avg Max Drawdown:         ${avg_drawdown:,.0f}\n"
    f"  Worst Max Drawdown:       ${max_drawdown:,.0f}\n\n"
    f"  EV/Hand:                  ${ev_per_hand:.4f}\n"
    f"  EV/Hour (150 rph):        ${ev_150rph:.2f}\n"
    f"{'═' * 42}"
)
ax.text(0.05, 0.95, stats_text, transform=ax.transAxes, fontsize=11,
        verticalalignment='top', fontfamily='monospace',
        bbox=dict(boxstyle='round,pad=0.8', facecolor='#f5f5f5', edgecolor='#333', alpha=0.9))

plt.subplots_adjust(top=0.93, bottom=0.05, left=0.05, right=0.97)
output_path = 'simulation_analysis.png'
plt.savefig(output_path, dpi=150, bbox_inches='tight')
plt.close()
print(f"Charts saved to: {output_path}")
