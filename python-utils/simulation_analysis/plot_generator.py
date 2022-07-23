from datetime import datetime
from pathlib import Path
from typing import Union

import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
import numpy as np
import pandas as pd
from matplotlib.ticker import FormatStrFormatter
from scipy import stats

from simulation_data import SimulationData

from plotutils import fig_size_ppt_full, fig_size_full_width_1_1, \
    format_plot_xlabels_days_6H, tum_colors, cm2inch


def generate_active_taxis_plot(simulation_data: SimulationData, reference_data: Union[pd.DataFrame, SimulationData],
                               save_dir: Path):
    fig, ax1 = plt.subplots(1, 1, figsize=fig_size_ppt_full)

    if isinstance(reference_data, pd.DataFrame):
        reference_label = "Database"
        reference_fleet_status = reference_data.count_active
        ax1.plot(reference_fleet_status, marker='.', markersize=0.2, label=reference_label, color=tum_colors[0][0])
        y_max = max(reference_fleet_status.max(), simulation_data.fleet_statistics.count_active.max())
    elif isinstance(reference_data, SimulationData):
        reference_label = "Reference"
        reference_data.fleet_statistics.plot_active_vehicles(
            ax=ax1,
            metadata=simulation_data.metadata,
            show_min=False,
            show_max=False,
            show_mean=False,
            label=reference_label,
            color=tum_colors[0][0]
        )
        y_max = max(reference_data.fleet_statistics.count_active.max(),
                    simulation_data.fleet_statistics.count_active.max())
    else:
        raise TypeError("Only Dataframes and SimulationData are supported as reference data")

    simulation_data.fleet_statistics.plot_active_vehicles(
        ax=ax1,
        metadata=simulation_data.metadata,
        show_min=True,
        show_max=True,
        show_mean=True,
        label="Simulation",
        color=tum_colors[2][1]
    )

    ax1.set_ylim(0, y_max + 100)
    format_plot_xlabels_days_6H(ax1)

    save_path = str(save_dir.joinpath("fleet_active_taxis.png").resolve())
    anotate_plot(fig, simulation_data)

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def generate_demand_plot(simulation_data: SimulationData,
                         save_dir: Path):
    # Plot number of pickups
    fig, ax1 = plt.subplots(1, 1, figsize=(15, 5))

    # Plot number of pickups
    fig, ax1 = plt.subplots(1, 1, figsize=(15, 5))
    ax1.plot(simulation_data.trip_statistics.get_pickup_ts(), marker='.', color=tum_colors[0][0])
    ax1.get_xaxis().set_label_text(None)
    ax1.set_ylabel("Number of pickups per 1 h ")
    ax1.set_ylim(0, ax1.get_ylim()[1])
    format_plot_xlabels_days_6H(ax1)

    save_path = str(save_dir.joinpath("requests_demand_over_time.png").resolve())
    anotate_plot(fig, simulation_data)

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def _generate_distance_share_from_sim_data(simulation_data: SimulationData, ax, title: str):
    reference_trip_distances = simulation_data.trip_statistics.distance_by_status_sum.copy()
    reference_trip_distances["distance"] = reference_trip_distances["distance"] / 1000
    reference_trip_distances["distance_cumsum"] = reference_trip_distances["distance"].cumsum()

    ax.invert_yaxis()
    ax.xaxis.set_visible(True)
    sim_summed_distance = reference_trip_distances["distance"].sum()
    ax.set_xlim(0, sim_summed_distance)
    ticks = list(ax.get_xticks())
    if sim_summed_distance - ticks[-2] < 65000:
        ticks = ticks[:-2]
    ticks.append(sim_summed_distance)
    ax.set_xticks(ticks)
    ax.xaxis.set_major_formatter(FormatStrFormatter("%d km"))

    for idx, (label, row) in enumerate(reference_trip_distances.iterrows()):
        widths = [row["distance"]]
        starts = [row["distance_cumsum"] - row["distance"]]
        color = tum_colors[2][idx]
        ax.barh([title], widths, left=starts, height=0.5, label=label, color=color)

        if row["distance"] / sim_summed_distance > 0.025:
            x_center = starts[0] + widths[0] / 2
            r, g, b = tuple(int(color.lstrip("#")[i:i + 2], 16) / 255 for i in (0, 2, 4))
            text_color = 'white' if r * g * b < 0.5 else 'dimgray'
            ax.text(x_center, 0, f"{row['distance']:.1f} km", ha="center",
                    va="center", color=text_color, fontdict={'rotation': 'vertical'})

    ax.legend(ncol=len(reference_trip_distances), loc='upper center', bbox_to_anchor=(0.5, 1.3))


def generate_distance_share_plot(simulation_data: SimulationData, reference_data: Union[pd.DataFrame, SimulationData],
                                 save_dir: Path):
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=fig_size_ppt_full)
    fig.subplots_adjust(hspace=0.5)

    if isinstance(reference_data, pd.DataFrame):
        reference_trips_df = reference_data
        reference_trip_distances = reference_trips_df.groupby(['type']).distance.sum().to_frame()
        reference_trip_distances["distance"] = reference_trip_distances["distance"] / 1000
        reference_trip_distances["distance_cumsum"] = reference_trip_distances["distance"].cumsum()
        ax1.invert_yaxis()
        ax1.xaxis.set_visible(True)
        reference_summed_distance = reference_trip_distances["distance"].sum()
        ax1.set_xlim(0, reference_summed_distance)
        ticks = list(ax1.get_xticks())
        if reference_summed_distance - ticks[-2] < 65000:
            ticks = ticks[:-2]
        ticks.append(reference_summed_distance)
        ax1.set_xticks(ticks)
        ax1.xaxis.set_major_formatter(FormatStrFormatter("%d km"))
        for idx, (label, row) in enumerate(reference_trip_distances.iterrows()):
            widths = [row["distance"]]
            starts = [row["distance_cumsum"] - row["distance"]]
            color = tum_colors[2][idx]
            ax1.barh(["Database"], widths, left=starts, height=0.5, label=label, color=color)

            if row["distance"] / reference_summed_distance > 0.025:
                x_center = starts[0] + widths[0] / 2
                r, g, b = tuple(int(color.lstrip("#")[i:i + 2], 16) / 255 for i in (0, 2, 4))
                text_color = 'white' if r * g * b < 0.5 else 'dimgray'
                ax1.text(x_center, 0, f"{row['distance']:.1f} km", ha="center",
                         va="center", color=text_color, fontdict={'rotation': 'vertical'})

        ax1.legend(ncol=len(reference_trips_df), loc='upper center', bbox_to_anchor=(0.5, 1.3))
    elif isinstance(reference_data, SimulationData):
        _generate_distance_share_from_sim_data(reference_data, ax1, "Reference")
    else:
        raise TypeError("Only Dataframes and SimulationData are supported as reference data")

    _generate_distance_share_from_sim_data(simulation_data, ax2, "Simulation")

    # make both x-axis the same max-limits
    x_max = ax1.get_xlim()[1]
    if x_max < ax2.get_xlim()[1]:
        x_max = ax2.get_xlim()[1]
    ax1.set_xlim([0, x_max])
    ax2.set_xlim([0, x_max])

    save_path = str(save_dir.joinpath("fleet_distance_share.png").resolve())
    anotate_plot(fig, simulation_data)

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def _generate_waiting_time_plot_for_sim_data(simulation_data, ax1, ax2, xx, label, color, max_minutes):
    waiting_times = simulation_data.service_quality.waiting_times[
        simulation_data.service_quality.waiting_times.dt.total_seconds() < max_minutes * 60
        ]
    ax1.hist(waiting_times.dt.total_seconds().values, density=True, bins=50,
             alpha=0.5, color=color, label=label)
    kde_reference = stats.gaussian_kde(waiting_times.dt.total_seconds().values)
    ax1.plot(xx, kde_reference(xx), color=color)

    counts, bin_edges = np.histogram(waiting_times.dt.total_seconds(), bins=100)
    cdf = np.cumsum(counts)
    ax2.plot(bin_edges[1:], cdf / cdf[-1], label=label, color=color)


def _add_line_to_legend(ax, legend, reference_label, type, values):
    old_handles, old_labels = ax.get_legend_handles_labels()
    old_handles.append(Line2D([0], [0], color=tum_colors[0][0], linestyle="dotted"))
    old_labels.append(f"{reference_label} {type} ({values[0]:.2f} s)")
    old_handles.append(Line2D([0], [0], color=tum_colors[2][1], linestyle="dotted"))
    old_labels.append(f"Simulation {type} ({values[1]:.2f} s)")
    legend._legend_box = None
    legend._init_legend_box(old_handles, old_labels)
    legend._set_loc(legend._loc)
    legend.set_title(legend.get_title().get_text())


def generate_waiting_time_plot(simulation_data: SimulationData, reference_data: Union[pd.DataFrame, SimulationData],
                               save_dir: Path):
    max_number_of_minutes = 30
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=fig_size_ppt_full)
    xx = np.linspace(-20, max_number_of_minutes * 60, 2000)

    if isinstance(reference_data, pd.DataFrame):
        reference_trips_df = reference_data
        reference_label = "Database"
        df_reference_approach = reference_trips_df[reference_trips_df.type == 'approach']
        waiting_times_reference = df_reference_approach.timestamp_stop - df_reference_approach.timestamp_start
        waiting_times_reference.index = df_reference_approach.timestamp_start
        reference_mean_waiting_time = waiting_times_reference.mean()

        # Filter values bigger than 30 min.
        waiting_times_reference = waiting_times_reference[waiting_times_reference.dt.total_seconds() <
                                                          max_number_of_minutes * 60]

        ax1.hist(waiting_times_reference.dt.total_seconds().values, density=True, bins=50,
                 alpha=0.5, color=tum_colors[0][0], label=reference_label)
        kde_reference = stats.gaussian_kde(waiting_times_reference.dt.total_seconds().values)
        ax1.plot(xx, kde_reference(xx), color=tum_colors[0][0])
        counts, bin_edges = np.histogram(waiting_times_reference.dt.total_seconds(), bins=100)
        cdf = np.cumsum(counts)
        ax2.plot(bin_edges[1:], cdf / cdf[-1], label=reference_label, color=tum_colors[0][0])
    elif isinstance(reference_data, SimulationData):
        reference_label = "Reference"
        _generate_waiting_time_plot_for_sim_data(
            simulation_data=reference_data,
            ax1=ax1,
            ax2=ax2,
            xx=xx,
            label=reference_label,
            color=tum_colors[0][0],
            max_minutes=max_number_of_minutes
        )
        reference_mean_waiting_time = reference_data.service_quality.waiting_times.mean()
    else:
        raise TypeError("Only Dataframes and SimulationData are supported as reference data")

    _generate_waiting_time_plot_for_sim_data(
        simulation_data=simulation_data,
        ax1=ax1,
        ax2=ax2,
        xx=xx,
        label="Simulation",
        color=tum_colors[2][1],
        max_minutes=max_number_of_minutes
    )
    sim_mean_waiting_time = simulation_data.service_quality.waiting_times.mean()

    ax1_legend = ax1.legend()
    ax1.set_ylabel("density")
    ax1.set_xlabel('waiting time in s')
    ax1.set_xlim(-20, max_number_of_minutes * 60)
    y_lim = ax1.get_ylim()
    plt.sca(ax1)
    plt.vlines(x=reference_mean_waiting_time.total_seconds(), ymin=y_lim[0], ymax=y_lim[1], color=tum_colors[0][0],
               linestyles="dotted")
    plt.vlines(x=sim_mean_waiting_time.total_seconds(), ymin=y_lim[0], ymax=y_lim[1], color=tum_colors[2][1],
               linestyles="dotted")
    ax1.set_ylim(y_lim[0], y_lim[1])

    plt.sca(ax2)
    y_lim = ax2.get_ylim()
    plt.vlines(x=reference_mean_waiting_time.total_seconds(), ymin=y_lim[0], ymax=y_lim[1], color=tum_colors[0][0],
               linestyles="dotted")
    plt.vlines(x=sim_mean_waiting_time.total_seconds(), ymin=y_lim[0], ymax=y_lim[1], color=tum_colors[2][1],
               linestyles="dotted")
    ax2.set_ylim(y_lim[0], y_lim[1])

    means = [reference_mean_waiting_time.total_seconds(), sim_mean_waiting_time.total_seconds()]
    _add_line_to_legend(ax=ax1, legend=ax1_legend, reference_label=reference_label, type='mean', values=means)

    ax2_legend = ax2.legend()
    ax2.set_ylabel("cumulative distribution")
    ax2.set_xlabel('waiting time in s')
    ax2.set_xlim(-20, max_number_of_minutes * 60)

    _add_line_to_legend(ax=ax2, legend=ax2_legend, reference_label=reference_label, type='mean', values=means)

    save_path = str(save_dir.joinpath("request_waiting_times_histogram.png").resolve())
    anotate_plot(fig, simulation_data)

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def generate_binned_waiting_times_plot(simulation_data: SimulationData,
                                       reference_data: Union[pd.DataFrame, SimulationData],
                                       save_dir: Path):
    if isinstance(reference_data, pd.DataFrame):
        reference_trips_df = reference_data
        df_reference_approach = reference_trips_df[reference_trips_df.type == 'approach']
        waiting_times_reference = df_reference_approach.timestamp_stop - df_reference_approach.timestamp_start
        waiting_times_reference.index = df_reference_approach.timestamp_start
        waiting_times_reference_15min = waiting_times_reference.dt.total_seconds().resample("15T")
        max_reference_waiting_time = waiting_times_reference_15min.apply(lambda x: x.quantile(0.95) / 60).max()
        reference_title = "Database"
    elif isinstance(reference_data, SimulationData):
        waiting_times_reference = reference_data.service_quality.waiting_times.dropna()[
                                  :simulation_data.metadata.simulation_end_time]
        waiting_times_reference_15min = waiting_times_reference.dt.total_seconds().resample("15T")
        max_reference_waiting_time = waiting_times_reference_15min.apply(lambda x: x.quantile(0.95) / 60).max()
        reference_title = "Reference"
    else:
        raise TypeError("Only Dataframes and SimulationData are supported as reference data")

    waiting_times_sim = simulation_data.service_quality.waiting_times.dropna()[
                        :simulation_data.metadata.simulation_end_time]
    waiting_times_sim_15min = waiting_times_sim.dt.total_seconds().resample("15T")
    max_sim_waiting_time = waiting_times_sim_15min.apply(lambda x: x.quantile(0.95) / 60).max()

    if max_sim_waiting_time > max_reference_waiting_time:
        max_wait_time = max_sim_waiting_time
    else:
        max_wait_time = max_reference_waiting_time

    max_y = max_wait_time + (10 - max_wait_time % 10)

    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(15, 10))
    fig.subplots_adjust(hspace=0.25)

    ax1.plot(waiting_times_reference_15min.apply(lambda x: x.quantile(0.10) / 60), color=tum_colors[0][0],
             label="10 % quantile")
    ax1.plot(waiting_times_reference_15min.apply(lambda x: x.quantile(0.50) / 60), color=tum_colors[2][1],
             label="50 % quantile")
    ax1.plot(waiting_times_reference_15min.apply(lambda x: x.quantile(0.95) / 60), color=tum_colors[2][2],
             label="95 % quantile")
    ax1.plot(waiting_times_reference_15min.mean() / 60, color=tum_colors[2][3],
             label="mean")
    format_plot_xlabels_days_6H(ax1)
    ax1.legend(ncol=4, loc='upper center', bbox_to_anchor=(0.5, 1))
    ax1.set_title(reference_title)
    ax1.set_ylim(-2, max_y)

    ax1.set_ylabel(f'waiting time in min ({reference_title})')

    ax2.plot(waiting_times_sim_15min.apply(lambda x: x.quantile(0.10) / 60), color=tum_colors[0][0],
             label="10 % quantile")
    ax2.plot(waiting_times_sim_15min.apply(lambda x: x.quantile(0.50) / 60), color=tum_colors[2][1],
             label="50 % quantile")
    ax2.plot(waiting_times_sim_15min.apply(lambda x: x.quantile(0.95) / 60), color=tum_colors[2][2],
             label="95 % quantile")
    ax2.plot(waiting_times_sim_15min.mean() / 60, color=tum_colors[2][3],
             label="mean")
    format_plot_xlabels_days_6H(ax2)
    ax2.legend(ncol=4, loc='upper center', bbox_to_anchor=(0.5, 1))
    ax2.set_title("Simulation")
    ax2.set_ylim(-2, max_y)

    ax2.set_ylabel('waiting time in min (Simulation)')

    save_path = str(save_dir.joinpath("request_waiting_times_over_time.png").resolve())
    anotate_plot(fig, simulation_data)

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def _count_simultaneous_relative_elements(df: pd.DataFrame, dt_start: datetime, dt_end: datetime, frequency: str,
                                          absolute=False):
    time_series = pd.date_range(start=dt_start, end=dt_end, freq=frequency)
    number_of_steps = len(time_series)
    number_of_trips = len(df)
    avg_trips_per_step = (number_of_trips / number_of_steps)
    df_counts = pd.DataFrame(index=time_series, columns=['relative_count'])

    iterator = df_counts.iterrows()
    present_index, present_values = next(iterator)

    for next_index, next_values in iterator:
        df_slice = df[(df.iloc[:, 0] <= next_index) & (df.iloc[:, 1] >= present_index)]
        if absolute:
            df_counts.loc[present_index] = len(df_slice)
        else:
            df_counts.loc[present_index] = len(df_slice) / avg_trips_per_step
        present_index = next_index

    return df_counts


def generate_waiting_customers_plot(simulation_data: SimulationData,
                                    reference_data: Union[pd.DataFrame, SimulationData],
                                    save_dir: Path):
    resampling_minutes = 60

    if isinstance(reference_data, pd.DataFrame):
        absolute = False
        reference_trips_df = reference_data
        df_reference_approach = reference_trips_df[reference_trips_df.type == 'approach']
        waiting_customers_reference = _count_simultaneous_relative_elements(
            df=df_reference_approach[['timestamp_start', 'timestamp_stop']],
            dt_start=simulation_data.metadata.simulation_start_time,
            dt_end=simulation_data.metadata.simulation_end_time,
            frequency=f"{resampling_minutes}T",
            absolute=absolute
        )
        reference_label = "Database"
    elif isinstance(reference_data, SimulationData):
        absolute = True
        waiting_customers_reference = _count_simultaneous_relative_elements(
            df=reference_data.trips[reference_data.trips.taxi_status == 'approach'][
                ['start_date_time', 'end_date_time']
            ],
            dt_start=simulation_data.metadata.simulation_start_time,
            dt_end=simulation_data.metadata.simulation_end_time,
            frequency=f"{resampling_minutes}T",
            absolute=absolute
        )
        reference_label = "Reference"
    else:
        raise TypeError("Only Dataframes and SimulationData are supported as reference data")

    waiting_customers_sim = _count_simultaneous_relative_elements(
        df=simulation_data.trips[simulation_data.trips.taxi_status == 'approach'][['start_date_time', 'end_date_time']],
        dt_start=simulation_data.metadata.simulation_start_time,
        dt_end=simulation_data.metadata.simulation_end_time,
        frequency=f"{resampling_minutes}T",
        absolute=absolute
    )

    fig, ax1 = plt.subplots(1, 1, figsize=fig_size_ppt_full)

    ax1.plot(waiting_customers_reference, label=reference_label, color=tum_colors[0][0])
    ax1.plot(waiting_customers_sim, label='Simulation', color=tum_colors[2][1])

    ax1.legend()
    if absolute:
        ax1.set_ylabel(f'Waiting customers\n(freq={resampling_minutes} min)')
    else:
        ax1.set_ylabel(f'Normalized waiting customers\n(freq={resampling_minutes} min)')
    format_plot_xlabels_days_6H(ax1)

    ax1.set_ylim(0, ax1.get_ylim()[1])
    save_path = str(save_dir.joinpath("request_waiting_customers_over_time.png").resolve())
    anotate_plot(fig, simulation_data)

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def generate_distance_distribution_plot(simulation_data: SimulationData,
                                        reference_data: Union[pd.DataFrame, SimulationData],
                                        save_dir: Path):
    fig, axes = plt.subplots(2, 2, figsize=fig_size_full_width_1_1)
    axes = [axis for sublist in axes for axis in sublist]

    if isinstance(reference_data, pd.DataFrame):
        df_ref = pd.DataFrame(reference_data[reference_data["type"] == "approach"]["distance"])
        df_ref["data_type"] = "Database"
        df_sim_data = pd.DataFrame(
            simulation_data.trips[simulation_data.trips["taxi_status"] == "approach"]["distance"]
        )
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["distance"], by="data_type", showfliers=False, ax=axes[0])
        axes[0].set_title("Approach")

        df_ref = pd.DataFrame(reference_data[reference_data["type"] == "idle"]["distance"])
        df_ref["data_type"] = "Database"
        df_sim_data = pd.DataFrame(
            simulation_data.trips[simulation_data.trips["taxi_status"] == "stay"]["distance"]
        )
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["distance"], by="data_type", showfliers=False, ax=axes[1])
        axes[1].set_title("Idle")

        df_ref = pd.DataFrame(reference_data[reference_data["type"] == "occupied"]["distance"])
        df_ref["data_type"] = "Database"
        df_sim_data = pd.DataFrame(
            simulation_data.trips[simulation_data.trips["taxi_status"] == "occupied"]["distance"]
        )
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["distance"], by="data_type", showfliers=False, ax=axes[2])
        axes[2].set_title("Occupied")

        df_ref = pd.DataFrame(reference_data[reference_data["type"] == "rebalancing"]["distance"])
        df_ref["data_type"] = "Database"
        df_sim_data = pd.DataFrame(
            simulation_data.trips[simulation_data.trips["taxi_status"] == "rebalancing"]["distance"]
        )
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["distance"], by="data_type", showfliers=False, ax=axes[3])
        axes[3].set_title("Rebalancing")
    elif isinstance(reference_data, SimulationData):
        df_ref = pd.DataFrame(reference_data.trips[reference_data.trips["taxi_status"] == "approach"]["distance"])
        df_ref["data_type"] = "Reference"
        df_sim_data = pd.DataFrame(
            simulation_data.trips[simulation_data.trips["taxi_status"] == "approach"]["distance"]
        )
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["distance"], by="data_type", showfliers=False, ax=axes[0])
        axes[0].set_title("Approach")

        df_ref = pd.DataFrame(reference_data.trips[reference_data.trips["taxi_status"] == "stay"]["distance"])
        df_ref["data_type"] = "Reference"
        df_sim_data = pd.DataFrame(simulation_data.trips[simulation_data.trips["taxi_status"] == "stay"]["distance"])
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["distance"], by="data_type", showfliers=False, ax=axes[1])
        axes[1].set_title("Idle")

        df_ref = pd.DataFrame(reference_data.trips[reference_data.trips["taxi_status"] == "occupied"]["distance"])
        df_ref["data_type"] = "Reference"
        df_sim_data = pd.DataFrame(
            simulation_data.trips[simulation_data.trips["taxi_status"] == "occupied"]["distance"]
        )
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["distance"], by="data_type", showfliers=False, ax=axes[2])
        axes[2].set_title("Occupied")

        df_ref = pd.DataFrame(reference_data.trips[reference_data.trips["taxi_status"] == "rebalancing"]["distance"])
        df_ref["data_type"] = "Reference"
        df_sim_data = pd.DataFrame(
            simulation_data.trips[simulation_data.trips["taxi_status"] == "rebalancing"]["distance"]
        )
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["distance"], by="data_type", showfliers=False, ax=axes[3])
        axes[3].set_title("Rebalancing")
    else:
        raise TypeError("Only Dataframes and SimulationData are supported as reference data")

    fig.suptitle(None)

    for ax in axes:
        ax.set_ylabel('Distance in m')
        ax.get_xaxis().set_label_text(None)

    save_path = str(save_dir.joinpath("trip_distance_boxplot.png").resolve())
    fig.tight_layout(pad=2)

    anotate_plot(fig, simulation_data)
    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def generate_duration_distribution_plot(simulation_data: SimulationData,
                                        reference_data: Union[pd.DataFrame, SimulationData],
                                        save_dir: Path):
    fig, axes = plt.subplots(2, 2, figsize=fig_size_full_width_1_1)
    axes = [axis for sublist in axes for axis in sublist]

    trip_durations_sim = simulation_data.trips.copy()
    trip_durations_sim["duration_min"] = trip_durations_sim.trip_statistics.duration.dt.total_seconds() / 60

    if isinstance(reference_data, pd.DataFrame):
        trip_durations = reference_data[['type', 'timestamp_start', 'timestamp_stop']].copy()

        trip_durations['duration_min'] = (trip_durations.timestamp_stop - trip_durations.timestamp_start
                                          ).dt.total_seconds() / 60

        df_ref = pd.DataFrame(trip_durations[trip_durations["type"] == "approach"]["duration_min"])
        df_ref["data_type"] = "Database"
        df_sim_data = pd.DataFrame(trip_durations_sim[trip_durations_sim["taxi_status"] == "approach"]["duration_min"])
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["duration_min"], by="data_type", showfliers=False, ax=axes[0])
        axes[0].set_title("Approach")

        df_ref = pd.DataFrame(trip_durations[trip_durations["type"] == "idle"]["duration_min"])
        df_ref["data_type"] = "Database"
        df_sim_data = pd.DataFrame(trip_durations_sim[trip_durations_sim["taxi_status"] == "stay"]["duration_min"])
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["duration_min"], by="data_type", showfliers=False, ax=axes[1])
        axes[1].set_title("Idle")

        df_ref = pd.DataFrame(trip_durations[trip_durations["type"] == "occupied"]["duration_min"])
        df_ref["data_type"] = "Database"
        df_sim_data = pd.DataFrame(trip_durations_sim[trip_durations_sim["taxi_status"] == "occupied"]["duration_min"])
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["duration_min"], by="data_type", showfliers=False, ax=axes[2])
        axes[2].set_title("Occupied")

        df_ref = pd.DataFrame(trip_durations[trip_durations["type"] == "rebalancing"]["duration_min"])
        df_ref["data_type"] = "Database"
        df_sim_data = pd.DataFrame(
            trip_durations_sim[trip_durations_sim["taxi_status"] == "rebalancing"]["duration_min"]
        )
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["duration_min"], by="data_type", showfliers=False, ax=axes[3])
        axes[3].set_title("Rebalancing")
    elif isinstance(reference_data, SimulationData):
        trip_durations = reference_data.trips.copy()
        trip_durations['duration_min'] = trip_durations.trip_statistics.duration.dt.total_seconds() / 60

        df_ref = pd.DataFrame(trip_durations[trip_durations["taxi_status"] == "approach"]["duration_min"])
        df_ref["data_type"] = "Reference"
        df_sim_data = pd.DataFrame(trip_durations_sim[trip_durations_sim["taxi_status"] == "approach"]["duration_min"])
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["duration_min"], by="data_type", showfliers=False, ax=axes[0])
        axes[0].set_title("Approach")

        df_ref = pd.DataFrame(trip_durations[trip_durations["taxi_status"] == "stay"]["duration_min"])
        df_ref["data_type"] = "Reference"
        df_sim_data = pd.DataFrame(trip_durations_sim[trip_durations_sim["taxi_status"] == "stay"]["duration_min"])
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["duration_min"], by="data_type", showfliers=False, ax=axes[1])
        axes[1].set_title("Idle")

        df_ref = pd.DataFrame(trip_durations[trip_durations["taxi_status"] == "occupied"]["duration_min"])
        df_ref["data_type"] = "Reference"
        df_sim_data = pd.DataFrame(trip_durations_sim[trip_durations_sim["taxi_status"] == "occupied"]["duration_min"])
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["duration_min"], by="data_type", showfliers=False, ax=axes[2])
        axes[2].set_title("Occupied")

        df_ref = pd.DataFrame(trip_durations[trip_durations["taxi_status"] == "rebalancing"]["duration_min"])
        df_ref["data_type"] = "Reference"
        df_sim_data = pd.DataFrame(
            trip_durations_sim[trip_durations_sim["taxi_status"] == "rebalancing"]["duration_min"]
        )
        df_sim_data["data_type"] = "Simulation"
        combined_df = pd.concat([df_ref, df_sim_data])
        combined_df.boxplot(column=["duration_min"], by="data_type", showfliers=False, ax=axes[3])
        axes[3].set_title("Rebalancing")
    else:
        raise TypeError("Only Dataframes and SimulationData are supported as reference data")

    fig.suptitle(None)
    for ax in axes:
        ax.set_ylabel('Duration in min')
        ax.get_xaxis().set_label_text(None)

    save_path = str(save_dir.joinpath("trip_duration_boxplot.png").resolve())
    fig.tight_layout(pad=2)
    anotate_plot(fig, simulation_data)

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def generate_speed_distribution_plot(simulation_data: SimulationData,
                                     reference_data: Union[pd.DataFrame, SimulationData],
                                     save_dir: Path):
    if isinstance(reference_data, pd.DataFrame):
        trip_speeds_reference = reference_data[['type', 'distance', 'timestamp_start', 'timestamp_stop']].copy()

        trip_speeds_reference['avg_speed_kmh'] = trip_speeds_reference.distance / 1000 / (
                (
                        trip_speeds_reference.timestamp_stop -
                        trip_speeds_reference.timestamp_start).dt.total_seconds() / 3600
        )
        reference_label = "Database"
        grouping_column = "type"
    elif isinstance(reference_data, SimulationData):
        trip_speeds_reference = reference_data.trips[['taxi_status', 'distance',
                                                      'start_date_time', 'end_date_time']].copy()

        trip_speeds_reference['avg_speed_kmh'] = trip_speeds_reference.distance / 1000 / (
                (trip_speeds_reference.end_date_time -
                 trip_speeds_reference.start_date_time).dt.total_seconds() / 3600
        )
        reference_label = "Reference"
        grouping_column = "taxi_status"
    else:
        raise TypeError("Only Dataframes and SimulationData are supported as reference data")

    fig, axes = plt.subplots(2, 2, figsize=fig_size_full_width_1_1)
    axes = [axis for sublist in axes for axis in sublist]

    trip_speeds_sim = simulation_data.trips[['taxi_status', 'distance', 'start_date_time', 'end_date_time']].copy()
    trip_speeds_sim['avg_speed_kmh'] = trip_speeds_sim.distance / 1000 / (
            (trip_speeds_sim.end_date_time - trip_speeds_sim.start_date_time).dt.total_seconds() / 3600)

    df_ref = pd.DataFrame(trip_speeds_reference[trip_speeds_reference[grouping_column] == "approach"]["avg_speed_kmh"])
    df_ref["data_type"] = reference_label
    df_sim_data = pd.DataFrame(trip_speeds_sim[trip_speeds_sim["taxi_status"] == "approach"]["avg_speed_kmh"])
    df_sim_data["data_type"] = "Simulation"
    combined_df = pd.concat([df_ref, df_sim_data])
    combined_df.boxplot(column=["avg_speed_kmh"], by="data_type", showfliers=False, ax=axes[0])
    axes[0].set_title("Approach")

    if isinstance(reference_data, pd.DataFrame):
        df_ref = pd.DataFrame(trip_speeds_reference[trip_speeds_reference[grouping_column] == "idle"]["avg_speed_kmh"])
    elif isinstance(reference_data, SimulationData):
        df_ref = pd.DataFrame(trip_speeds_reference[trip_speeds_reference[grouping_column] == "stay"]["avg_speed_kmh"])
    else:
        raise TypeError("Only Dataframes and SimulationData are supported as reference data")
    df_ref["data_type"] = reference_label
    df_sim_data = pd.DataFrame(trip_speeds_sim[trip_speeds_sim["taxi_status"] == "stay"]["avg_speed_kmh"])
    df_sim_data["data_type"] = "Simulation"
    combined_df = pd.concat([df_ref, df_sim_data])
    combined_df.boxplot(column=["avg_speed_kmh"], by="data_type", showfliers=False, ax=axes[1])
    axes[1].set_title("Idle")

    df_ref = pd.DataFrame(trip_speeds_reference[trip_speeds_reference[grouping_column] == "occupied"]["avg_speed_kmh"])
    df_ref["data_type"] = reference_label
    df_sim_data = pd.DataFrame(trip_speeds_sim[trip_speeds_sim["taxi_status"] == "occupied"]["avg_speed_kmh"])
    df_sim_data["data_type"] = "Simulation"
    combined_df = pd.concat([df_ref, df_sim_data])
    combined_df.boxplot(column=["avg_speed_kmh"], by="data_type", showfliers=False, ax=axes[2])
    axes[2].set_title("Occupied")

    df_ref = pd.DataFrame(
        trip_speeds_reference[trip_speeds_reference[grouping_column] == "rebalancing"]["avg_speed_kmh"]
    )
    df_ref["data_type"] = reference_label
    df_sim_data = pd.DataFrame(trip_speeds_sim[trip_speeds_sim["taxi_status"] == "rebalancing"]["avg_speed_kmh"])
    df_sim_data["data_type"] = "Simulation"
    combined_df = pd.concat([df_ref, df_sim_data])
    combined_df.boxplot(column=["avg_speed_kmh"], by="data_type", showfliers=False, ax=axes[3])
    axes[3].set_title("Rebalancing")

    fig.suptitle(None)
    for ax in axes:
        ax.set_ylabel('Avg speed in km/h')
        ax.get_xaxis().set_label_text(None)

    save_path = str(save_dir.joinpath("trip_speed_boxplot.png").resolve())
    fig.tight_layout(pad=2)

    anotate_plot(fig, simulation_data)

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def generate_fleet_status_plot(simulation_data: SimulationData, reference_data: Union[pd.DataFrame, SimulationData],
                               save_dir: Path):
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=cm2inch(32, 20), sharey=True)
    fig.subplots_adjust(hspace=0.2)

    local_sim_data = simulation_data.fleet.dropna()[:simulation_data.metadata.simulation_end_time].copy()
    sim_fleet_status = local_sim_data.resample("15T").mean()

    ax1.set_prop_cycle(color=tum_colors[2])
    if isinstance(reference_data, pd.DataFrame):
        reference_fleet_status = reference_data.resample("15T").mean()
        reference_fleet_status = reference_fleet_status.iloc[:, 1:]
        column_order = [
            "count_occupied",
            "count_approach",
            "count_rebalancing",
            "count_idle",
            "count_waiting_at_pickup"
        ]
        reference_fleet_status = reference_fleet_status.reindex(column_order, axis=1)
        reference_label = "Database"
        reference_legend_labels = ['Occupied', 'Approach', 'Rebalancing', 'Free', 'Waiting at pickup']
        ax1.stackplot(reference_fleet_status.index, reference_fleet_status.T)
    elif isinstance(reference_data, SimulationData):
        reference_fleet_status = reference_data.fleet.dropna()[:simulation_data.metadata.simulation_end_time].copy()
        reference_fleet_status = reference_fleet_status.resample("15T").mean()
        reference_label = "Reference"
        reference_legend_labels = ['Occupied', 'Approach', 'Rebalancing', 'Free']
        ax1.stackplot(reference_fleet_status.index, reference_fleet_status.iloc[:, :4].T)
    else:
        raise TypeError("Only Dataframes and SimulationData are supported as reference data")

    format_plot_xlabels_days_6H(ax1)
    ax1.legend(reference_legend_labels,
               ncol=len(reference_legend_labels), loc='upper center', bbox_to_anchor=(0.5, 1))
    ax1.set_title(reference_label)
    ax1.set_ylabel("No. of vehicles")

    ax2.set_prop_cycle(color=tum_colors[2])
    ax2.stackplot(sim_fleet_status.index, sim_fleet_status.iloc[:, :4].T)
    format_plot_xlabels_days_6H(ax2)
    ax2.legend(['Occupied', 'Approach', 'Rebalancing', 'Free'],
               ncol=4, loc='upper center', bbox_to_anchor=(0.5, 1))
    ax2.set_title("Simulation")
    ax2.set_ylabel("No. of vehicles")

    save_path = str(save_dir.joinpath("fleet_status.png").resolve())
    fig.tight_layout()
    anotate_plot(fig, simulation_data)

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def generate_occupancy_ratio_plot(simulation_data: SimulationData,
                                  reference_data: Union[pd.DataFrame, SimulationData],
                                  save_dir: Path):
    resampling_minutes = 15
    fig, (ax1) = plt.subplots(1, 1, figsize=fig_size_ppt_full)

    if isinstance(reference_data, pd.DataFrame):
        reference_fleet_status_15min = reference_data.resample(f"{resampling_minutes}T").mean()

        time_series = pd.date_range(
            start=simulation_data.metadata.simulation_start_time,
            end=simulation_data.metadata.simulation_end_time,
            freq=f"{resampling_minutes}T"
        )
        reference_fleet_utilization = pd.DataFrame(index=time_series, columns=['time'])
        reference_fleet_utilization['time'] = reference_fleet_status_15min.count_occupied / \
            reference_fleet_status_15min.count_active

        ax1.plot(reference_fleet_utilization["time"], label='Database', color=tum_colors[0][0])
    elif isinstance(reference_data, SimulationData):
        ax1.plot(reference_data.fleet_statistics.get_occupancy_over_time_ts(frequency=f"{resampling_minutes}T").dropna()
                 [:simulation_data.metadata.simulation_end_time][1:],
                 label='Reference',
                 color=tum_colors[0][0]
                 )
    else:
        raise TypeError("Only Dataframes and SimulationData are supported as reference data")

    ax1.plot(simulation_data.fleet_statistics.get_occupancy_over_time_ts(
        frequency=f"{resampling_minutes}T"
    ).dropna()[:simulation_data.metadata.simulation_end_time][1:],
             label='Simulation',
             color=tum_colors[2][1]
             )

    ax1.legend()
    ax1.set_ylabel('Fleet occupancy ratio \n (occupied/total vehicles)')
    format_plot_xlabels_days_6H(ax1)
    ax1.set_ylim(0, 1)
    save_path = str(save_dir.joinpath("fleet_occupancy_ratio.png").resolve())
    fig.tight_layout()
    anotate_plot(fig, simulation_data)

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def generate_nonavailability_ratio_plot(simulation_data: SimulationData,
                                        reference_data: Union[pd.DataFrame, SimulationData],
                                        save_dir: Path):
    resampling_minutes = 15
    fig, (ax1) = plt.subplots(1, 1, figsize=fig_size_ppt_full)

    if isinstance(reference_data, pd.DataFrame):
        reference_fleet_status_15min = reference_data.resample(f"{resampling_minutes}T").mean()

        time_series = pd.date_range(
            start=simulation_data.metadata.simulation_start_time,
            end=simulation_data.metadata.simulation_end_time,
            freq=f"{resampling_minutes}T"
        )
        reference_fleet_utilization = pd.DataFrame(index=time_series, columns=['time'])
        reference_fleet_utilization['time'] = (
                                                      reference_fleet_status_15min.count_occupied +
                                                      reference_fleet_status_15min.count_approach +
                                                      reference_fleet_status_15min.count_waiting_at_pickup) / \
            reference_fleet_status_15min.count_active
        ax1.plot(reference_fleet_utilization["time"], label='Database', color=tum_colors[0][0])
    elif isinstance(reference_data, SimulationData):
        ax1.plot(reference_data.fleet_statistics.get_nonavailability_over_time_ts(
            frequency=f"{resampling_minutes}T").dropna()
                 [:simulation_data.metadata.simulation_end_time][1:],
                 label='Reference',
                 color=tum_colors[0][0]
                 )
    else:
        raise TypeError("Only Dataframes and SimulationData are supported as reference data")

    ax1.plot(simulation_data.fleet_statistics.get_nonavailability_over_time_ts(
        frequency=f"{resampling_minutes}T"
    ).dropna()[:simulation_data.metadata.simulation_end_time][1:],
             label='Simulation',
             color=tum_colors[2][1]
             )

    ax1.legend()
    ax1.set_ylabel('Fleet nonavailability ratio \n (blocked/total vehicles)')
    format_plot_xlabels_days_6H(ax1)
    ax1.set_ylim(0, 1)
    save_path = str(save_dir.joinpath("fleet_nonavailability_ratio.png").resolve())
    fig.tight_layout()
    anotate_plot(fig, simulation_data)

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def generate_distance_ratio_plot(simulation_data: SimulationData, reference_data: Union[pd.DataFrame, SimulationData],
                                 save_dir: Path):
    resampling_minutes = 60

    if isinstance(reference_data, pd.DataFrame):
        reference_trips_df = reference_data
        reference_dist_total = reference_trips_df["distance"]
        reference_dist_total.index = reference_trips_df["timestamp_stop"]
        reference_dist_total = reference_dist_total.resample(f"{resampling_minutes}T").sum()

        reference_dist_occupied = reference_trips_df[reference_trips_df["type"] == "occupied"]["distance"]
        reference_dist_occupied.index = reference_trips_df[reference_trips_df["type"] == "occupied"]["timestamp_stop"]
        reference_dist_occupied = reference_dist_occupied.resample(f"{resampling_minutes}T").sum()
        reference_dist_occupied = reference_dist_occupied.reindex(reference_dist_total.index, fill_value=0)
        reference_dist_occupied = reference_dist_occupied.dropna()[:simulation_data.metadata.simulation_end_time]
        reference_label = "Database"
    elif isinstance(reference_data, SimulationData):
        reference_dist_total = reference_data.trips["distance"]
        reference_dist_total.index = reference_data.trips["end_date_time"]
        reference_dist_total = reference_dist_total.resample(f"{resampling_minutes}T").sum()

        reference_dist_occupied = reference_data.trips[reference_data.trips["taxi_status"] == "occupied"]["distance"]
        reference_dist_occupied.index = reference_data.trips[reference_data.trips["taxi_status"] == "occupied"][
            "end_date_time"]
        reference_dist_occupied = reference_dist_occupied.resample(f"{resampling_minutes}T").sum()
        reference_dist_occupied = reference_dist_occupied.reindex(reference_dist_total.index, fill_value=0)
        reference_dist_occupied = reference_dist_occupied.dropna()[:simulation_data.metadata.simulation_end_time][1:]
        reference_label = "Reference"
    else:
        raise TypeError("Only Dataframes and SimulationData are supported as reference data")

    sim_dist_total = simulation_data.trips["distance"]
    sim_dist_total.index = simulation_data.trips["end_date_time"]
    sim_dist_total = sim_dist_total.resample(f"{resampling_minutes}T").sum()

    sim_dist_occupied = simulation_data.trips[simulation_data.trips["taxi_status"] == "occupied"]["distance"]
    sim_dist_occupied.index = simulation_data.trips[simulation_data.trips["taxi_status"] == "occupied"]["end_date_time"]
    sim_dist_occupied = sim_dist_occupied.resample(f"{resampling_minutes}T").sum()
    sim_dist_occupied = sim_dist_occupied.reindex(sim_dist_total.index, fill_value=0)
    sim_dist_occupied = sim_dist_occupied.dropna()[:simulation_data.metadata.simulation_end_time][1:]

    fig, ax1 = plt.subplots(1, 1, figsize=fig_size_ppt_full)

    ax1.plot(
        reference_dist_occupied / reference_dist_total,
        label=reference_label,
        color=tum_colors[0][0]
    )
    ax1.plot(
        sim_dist_occupied / sim_dist_total,
        label='Simulation',
        color=tum_colors[2][1]
    )

    ax1.legend()
    ax1.set_ylabel('Fleet distance ratio (occupied/total km)')
    ax1.set_ylim(0, 1)
    format_plot_xlabels_days_6H(ax1)

    save_path = str(save_dir.joinpath("fleet_distance_ratio.png").resolve())
    fig.tight_layout()
    anotate_plot(fig, simulation_data)

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def generate_cost_revenue_plot(simulation_data: SimulationData, reference_data: Union[pd.DataFrame, SimulationData],
                               save_dir: Path):
    if isinstance(reference_data, pd.DataFrame):
        reference_trips_df = reference_data
        reference_revenue_costs = reference_trips_df[["fare_distance", "fare_waiting", "fare_base",
                                                      "costs_distance", "costs_time"]].copy()
        reference_revenue_costs.index = reference_trips_df["timestamp_start"]
        reference_revenue_costs["revenue"] = (reference_revenue_costs["fare_distance"]
                                              + reference_revenue_costs["fare_waiting"]
                                              + reference_revenue_costs["fare_base"])
        reference_revenue_costs["costs"] = (reference_revenue_costs["costs_distance"]
                                            + reference_revenue_costs["costs_time"])
        reference_revenue_costs = reference_revenue_costs[["revenue", "costs"]]
        reference_revenue_costs = reference_revenue_costs.cumsum()
    elif isinstance(reference_data, SimulationData):
        reference_revenue_costs = reference_data.trips[["revenue", "costs"]].copy()
        reference_revenue_costs.index = reference_data.trips["start_date_time"]
        reference_revenue_costs = reference_revenue_costs.sort_index()
        reference_revenue_costs = reference_revenue_costs.cumsum()
    else:
        raise TypeError("Only Dataframes and SimulationData are supported as reference data")

    sim_revenue_costs = simulation_data.trips[["revenue", "costs"]].copy()
    sim_revenue_costs.index = simulation_data.trips["start_date_time"]
    sim_revenue_costs = sim_revenue_costs.sort_index()
    sim_revenue_costs = sim_revenue_costs.cumsum()

    fig, ax1 = plt.subplots(1, 1, figsize=fig_size_ppt_full)

    ax1.plot(reference_revenue_costs["revenue"], linestyle='dashed', label="Reference Revenue", color=tum_colors[0][0])
    ax1.plot(sim_revenue_costs["revenue"], label="Simulation Revenue", color=tum_colors[0][0])
    ax1.plot(reference_revenue_costs["costs"], linestyle='dashed', label="Reference Costs", color=tum_colors[2][1])
    ax1.plot(sim_revenue_costs["costs"], label="Simulation Costs", color=tum_colors[2][1])
    ax1.set_title("")
    ax1.set_ylabel("Cumulated Costs/Revenue")
    ax1.yaxis.set_major_formatter(FormatStrFormatter("%d €"))
    ax1.legend()

    format_plot_xlabels_days_6H(ax1)

    save_path = str(save_dir.joinpath("fleet_costs_revenue.png").resolve())
    fig.tight_layout()
    anotate_plot(fig, simulation_data)

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def generate_cancelled_rides_bar_plot(simulation_data: SimulationData, save_dir: Path, reference_data=None):
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=fig_size_ppt_full, sharex=True)
    ax2.set_xlabel("No. of requests")

    if isinstance(reference_data, SimulationData):
        label = ['Reference']
        ref_denied_requests = reference_data.service_quality.denied_requests
        served_requests = ref_denied_requests[~ref_denied_requests].shape[0]
        cancelled_requests = ref_denied_requests[ref_denied_requests].shape[0]
        ratio = cancelled_requests/ref_denied_requests.shape[0] * 100

        ax1.barh(label, served_requests, label=f'Served requests (n={served_requests})')
        ax1.barh(label, cancelled_requests, label=f'Cancelled requests (n={cancelled_requests})',
                 left=served_requests)

        ax1.text(served_requests/2, 0, f"{100-ratio:.2f} %", ha="center",
                 va="center", color='white', fontdict={'rotation': 'vertical'})
        ax1.text(served_requests + cancelled_requests / 2, 0, f"{ratio:.2f} %", ha="center",
                 va="center", color='white', fontdict={'rotation': 'vertical'})
        ax1.legend(ncol=2, loc='upper center', bbox_to_anchor=(0.5, 1.3))
    else:
        ax1.remove()

    label = ['Simulation']
    sim_denied_requests = simulation_data.service_quality.denied_requests
    served_requests = sim_denied_requests[~sim_denied_requests].shape[0]
    cancelled_requests = sim_denied_requests[sim_denied_requests].shape[0]
    ratio = cancelled_requests / sim_denied_requests.shape[0] * 100

    ax2.barh(label, served_requests, label=f'Served requests (n={served_requests})')
    ax2.barh(label, cancelled_requests, label=f'Cancelled requests (n={cancelled_requests})',
             left=served_requests)

    ax2.text(served_requests / 2, 0, f"{100-ratio:.2f} %", ha="center",
             va="center", color='white', fontdict={'rotation': 'vertical'})
    ax2.text(served_requests + cancelled_requests / 2, 0, f"{ratio:.2f} %", ha="center",
             va="center", color='white', fontdict={'rotation': 'vertical'})
    ax2.legend(ncol=2, loc='upper center', bbox_to_anchor=(0.5, 1.3))
    # for p in ax1.patches:
    #     ax1.annotate(str(p.get_height()), (p.get_x() * 1.005, p.get_height() * 1.005))'
    ax2.set_xlim(0, sim_denied_requests.shape[0])

    save_path = str(save_dir.joinpath("request_cancelled_bar.png").resolve())
    anotate_plot(fig, simulation_data)
    # fig.tight_layout()

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)
    pass


def generate_cancelled_trips_time_series_plot(simulation_data: SimulationData, reference_data: SimulationData,
                                              save_dir: Path):
    fig, ax1 = plt.subplots(1, 1, figsize=fig_size_ppt_full)
    idx = pd.period_range(simulation_data.metadata.simulation_start_time, simulation_data.metadata.simulation_end_time,
                          freq='5T')
    # results.reindex(idx, fill_value=0)
    df_sim_denied = simulation_data.requests.denied
    df_sim_denied.index = simulation_data.requests.timestamp_submitted
    df_sim_denied = df_sim_denied[df_sim_denied]
    df_sim_denied = df_sim_denied.resample('5T').count()
    df_sim_denied = df_sim_denied.reindex(idx.start_time, fill_value=0)
    bar_width = 0.5 if df_sim_denied.shape[0] == 0 else 0.5 * (1 / df_sim_denied.shape[0])
    ax1.bar(df_sim_denied.index, df_sim_denied.values, width=bar_width,
            label=f'Simulation (n={df_sim_denied.sum()})',
            color=tum_colors[0][0])

    if reference_data is not None:
        df_ref_denied = reference_data.requests.denied
        df_ref_denied.index = reference_data.requests.timestamp_submitted
        df_ref_denied = df_ref_denied[df_ref_denied]
        df_ref_denied = df_ref_denied.resample('5T').count()
        df_ref_denied.reindex(idx.start_time, fill_value=0)
        bar_width = 0.5 if df_ref_denied.shape[0] == 0 else 0.5 * (1 / df_ref_denied.shape[0])
        ax1.bar(df_ref_denied.index, df_ref_denied, width=bar_width, label=f'Reference (n={df_ref_denied.sum()})',
                color=tum_colors[2][1])

    ax1.legend()
    ax1.set_ylabel('Canceled Requests over time')
    # ax1.set_ylim(0, 1)
    format_plot_xlabels_days_6H(ax1)

    save_path = str(save_dir.joinpath("request_cancelled_over_time.png").resolve())
    anotate_plot(fig, simulation_data)
    fig.tight_layout()

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def generate_assignment_duration_plot(simulation_data: SimulationData, reference_data: SimulationData,
                                      save_dir: Path):
    max_number_of_minutes = 30
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=fig_size_ppt_full)
    xx = np.linspace(-20, max_number_of_minutes * 60, 2000)

    if reference_data is not None:
        reference_label = "Reference"
        _generate_assigment_time_plot_for_sim_data(
            simulation_data=reference_data,
            ax1=ax1,
            ax2=ax2,
            xx=xx,
            label=reference_label,
            color=tum_colors[0][0],
        )
        reference_mean_waiting_time = reference_data.service_quality.dispatching_times.mean()

    _generate_assigment_time_plot_for_sim_data(
        simulation_data=simulation_data,
        ax1=ax1,
        ax2=ax2,
        xx=xx,
        label="Simulation",
        color=tum_colors[2][1],
    )
    sim_mean_assignment_time = simulation_data.service_quality.dispatching_times.mean()

    ax1_legend = ax1.legend()
    ax1.set_ylabel("density")
    ax1.set_xlabel('assignment time in s')
    ax1.set_xlim(-20, max_number_of_minutes * 60)
    y_lim = ax1.get_ylim()
    plt.sca(ax1)

    plt.vlines(x=sim_mean_assignment_time.total_seconds(), ymin=y_lim[0], ymax=y_lim[1], color=tum_colors[2][1],
               linestyles="dotted")
    ax1.set_ylim(y_lim[0], y_lim[1])

    plt.sca(ax2)
    y_lim = ax2.get_ylim()

    plt.vlines(x=sim_mean_assignment_time.total_seconds(), ymin=y_lim[0], ymax=y_lim[1], color=tum_colors[2][1],
               linestyles="dotted")
    if reference_data is not None:
        plt.vlines(x=reference_mean_waiting_time.total_seconds(), ymin=y_lim[0], ymax=y_lim[1], color=tum_colors[0][0],
                   linestyles="dotted")
        means = [reference_mean_waiting_time.total_seconds(), sim_mean_assignment_time.total_seconds()]
        _add_line_to_legend(ax=ax1, legend=ax1_legend, reference_label=reference_label, type='mean', values=means)
    # else:
    #     means = [sim_mean_assignment_time.total_seconds()]
    #     _add_line_to_legend(ax=ax1, legend=ax1_legend, reference_label="", type='mean', values=means)
    ax2.set_ylim(0, 1)
    # ax2_legend = ax2.legend()
    ax2.set_ylabel("cumulative distribution")
    ax2.set_xlabel('assignment time in s')
    ax2.set_xlim(-20, max_number_of_minutes * 60)

    #    _add_line_to_legend(ax=ax2, legend=ax2_legend, reference_label=reference_label, type='mean', values=means)

    save_path = str(save_dir.joinpath("request_assignment_times_histogram.png").resolve())
    anotate_plot(fig, simulation_data)

    plt.savefig(save_path, dpi=300)
    plt.close(fig=fig)


def _generate_assigment_time_plot_for_sim_data(simulation_data, ax1, ax2, xx, label, color):
    assignment = simulation_data.service_quality.dispatching_times
    ax1.hist(assignment.dt.total_seconds().values, density=True, bins=50,
             alpha=0.5, color=color, label=label)
    kde_reference = stats.gaussian_kde(assignment.dt.total_seconds().values)
    ax1.plot(xx, kde_reference(xx), color=color)

    counts, bin_edges = np.histogram(assignment.dt.total_seconds(), bins=100)
    cdf = np.cumsum(counts)
    ax2.plot(bin_edges[1:], cdf / cdf[-1], label=label, color=color)


def anotate_plot(fig, simulation_data: SimulationData):
    fig.suptitle(" ")
    fig.tight_layout()
    plt.annotate(simulation_data.metadata.label(),
                 (0.05, 1), (0, -10), xycoords='figure fraction',
                 textcoords='offset points', va='top', fontsize=8)
