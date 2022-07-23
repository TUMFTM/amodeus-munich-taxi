import pandas as pd
from datetime import datetime
from lxml import etree
import re


def generate_demand_prediction_xml(predictions_path, output_path,
                                   start_time: datetime, stop_time: datetime,
                                   relative_time=True):

    df_grid = pd.read_hdf(predictions_path, key='grid_predictions')  # type: pd.DataFrame
    df_border = pd.read_hdf(predictions_path, key='border_predictions')  # type: pd.DataFrame

    df_grid = df_grid[start_time:stop_time]
    df_border = df_border[start_time:stop_time]

    n_rows = int(re.findall(r'\d+', df_grid.columns[-1])[0]) + 1
    n_cols = int(re.findall(r'\d+', df_grid.columns[-1])[1]) + 1
    x_min = 687412
    x_max = 695412
    y_min = 5331314
    y_max = 5339314
    border_width = 5000
    prediction_windows = len(df_grid.iloc[0, 0])
    prediction_horizon = pd.to_timedelta(df_grid.index.freq).total_seconds()
    e_demand_prediction = etree.Element("uniform_grid_with_border_predictions")
    e_demand_prediction.set("start_date", df_grid.index[0].strftime('%Y-%m-%d %H:%M:%S'))
    e_demand_prediction.set("stop_date", df_grid.index[-1].strftime('%Y-%m-%d %H:%M:%S'))
    e_demand_prediction.set("n_grid_rows", str(n_rows))
    e_demand_prediction.set("n_grid_columns", str(n_cols))
    e_demand_prediction.set("borderWidth", str(border_width))
    e_demand_prediction.set("x_min", str(x_min))
    e_demand_prediction.set("x_max", str(x_max))
    e_demand_prediction.set("y_min", str(y_min))
    e_demand_prediction.set("y_max", str(y_max))
    e_demand_prediction.set("prediction_windows", str(prediction_windows))
    e_demand_prediction.set("prediction_horizon", str(prediction_horizon))

    for time_step in range(df_grid.shape[0]):
        e_prediction = etree.SubElement(e_demand_prediction, "prediction")
        time = df_grid.index[time_step]
        if relative_time:
            e_prediction.set("time", str((time-start_time).total_seconds()))
        else:
            e_prediction.set("time", time.strftime('%Y-%m-%d %H:%M:%S'))
        e_grid = etree.SubElement(e_prediction, "grid_cells")
        for col in df_grid.columns:
            e_cell = etree.SubElement(e_grid, "cell")
            e_cell.set("id", col)
            for yhat in range(prediction_windows):
                e_yhat = etree.SubElement(e_cell, "y_hat")
                e_yhat.set("t", str(yhat))
                e_yhat.set("value", str(df_grid[col].iloc[time_step][yhat]))
        e_border = etree.SubElement(e_prediction, "border_cells")

        for col in df_border.columns:
            e_cell = etree.SubElement(e_border, "cell")
            e_cell.set("id", col)
            for yhat in range(prediction_windows):
                e_yhat = etree.SubElement(e_cell, "y_hat")
                e_yhat.set("t", str(yhat))
                e_yhat.set("value", str(df_border[col].iloc[time_step][yhat]))

    output_file = open(output_path, "wb")
    output_file.write('<!DOCTYPE uniform_grid_with_border_predictions SYSTEM '
                      '"https://raw.githubusercontent.com/michaelwittmann/'
                      'amodeus-taxi-munich-dtd/master/demand_prediction.dtd">\n'.encode('UTF-8'))
    output_file.write(etree.tostring(e_demand_prediction, pretty_print=True))
    output_file.close()


if __name__ == '__main__':
    input = filepath = "INPUT_YOUR_FILE_PATH_HERE"
    output = "predictions.xml"

    start_date = datetime(2016, 11, 7, 6, 0, 0) #<< ADJUST TO YOUR NEEEDS
    stop_date = datetime(2016, 11, 7, 8, 0, 0) #<< ADJUST TO YOUR NEEDS
    generate_demand_prediction_xml(predictions_path=input, output_path=output,
                                   start_time=start_date, stop_time=stop_date, relative_time=True)
