import matplotlib.dates
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd


# Helper Functions
def percentile(n):
    def percentile_(x):
        return np.percentile(x, n)

    percentile_.__name__ = 'percentile_%s' % n
    return percentile_


def share_with_NaN(value, base):
    if base == 0:
        return float('NaN')
    else:
        return value / base


def cm2inch(*tupl):
    inch = 2.54
    if isinstance(tupl[0], tuple):
        return tuple(i / inch for i in tupl[0])
    else:
        return tuple(i / inch for i in tupl)


def format_plot_xlabels_days_6H(ax):
    ax.xaxis.set_major_locator(matplotlib.dates.DayLocator())
    ax.xaxis.set_minor_locator(matplotlib.dates.HourLocator(byhour=[6, 12, 18]))
    ax.xaxis.set_major_formatter(matplotlib.dates.DateFormatter('%a'))
    ax.xaxis.set_minor_formatter(matplotlib.dates.DateFormatter('%H'))

def format_plot_xlabels_week_ts(ax):
    ax.xaxis.set_major_locator(matplotlib.dates.DayLocator())
    ax.xaxis.set_minor_locator(matplotlib.dates.HourLocator(byhour=[0, 6, 12, 18]))
    ax.xaxis.set_major_formatter(matplotlib.dates.DateFormatter('%a.'))
    ax.xaxis.set_minor_formatter(matplotlib.dates.DateFormatter('%H:%M'))
    ax.tick_params(axis='x', which='minor', labelsize=6, labelrotation=90)
    ax.tick_params(axis='x', which='major', labelrotation=90)


def plot_cdf(data: pd.Series, ax=None, bins=100, label=None):
    if ax is None:
        fig, ax = new_figure()
    counts, bin_edges = np.histogram(data, bins=100)
    cdf = np.cumsum(counts)
    ax.plot(bin_edges[1:], cdf / cdf[-1], label=label)


tum_colors = [['#0065BD', '#000000', '#ffffff'],
              ['#005293', '#003359', '#333333', '#808080', '#CCCCC6'],
              ['#DAD7CB', '#E37222', '#A2AD00', '#98C6EA', '#64A0C8'],
              ['#69085a', '#0f1b5f', '#00778a', '#007c30', '#679a1d', '#ffdc00', '#f9ba00',
               '#d64c13', '#c4071b', '#9c0d16']]

# Definitions
fig_page_width = cm2inch(16, 8)
fig_size_full_width_2_1 = cm2inch(14.4, 7.2)
fig_size_full_width_1_1 = cm2inch(14.4, 14.4)
fig_size_full_width_4_3 = cm2inch(14.4, 10.8)
fig_size_full_width_16_9 = cm2inch(14.4, 8.1)
fig_size_half_width_1_1 = cm2inch(6.7, 6.7)

fig_size_ppt_full = cm2inch(32, 11)
fig_size_ppt_half = cm2inch(16.3, 11)
fig_size_ppt_half_1_1 = cm2inch(11, 11)


def new_figure(figsize=fig_page_width):
    return plt.subplots(1, 1, figsize=figsize)
