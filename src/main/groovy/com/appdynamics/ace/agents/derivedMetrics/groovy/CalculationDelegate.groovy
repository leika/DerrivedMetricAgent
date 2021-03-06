package com.appdynamics.ace.agents.derivedMetrics.groovy

import com.appdynamics.ace.com.appdynamics.ace.agents.derivedMetrics.java.CalculationException
import com.appdynamics.ace.com.appdynamics.ace.agents.derivedMetrics.java.MetricValueContainer
import de.appdynamics.ace.metric.query.data.DataMap
import de.appdynamics.ace.metric.query.data.DataRow
import de.appdynamics.ace.metric.query.data.TextColumn
import de.appdynamics.ace.metric.query.data.ValueColumn
import de.appdynamics.ace.metric.query.data.ValueDataObject
import de.appdynamics.ace.metric.query.data.TimestampColumn
import de.appdynamics.ace.metric.query.data.TimestampDataObject
import org.apache.log4j.Logger

/**
 * Created by stefan.marx on 18.04.16.
 */
class CalculationDelegate extends CalculationDelegateBase {

    private DataMap _allData
    private DataMap _filteredData




    public CalculationDelegate (DataMap allData, DataMap filteredData) {

        this._filteredData = filteredData
        this._allData = allData

    }

    public void dumpData() {
        getLogger().info("Data:\n"+_filteredData.dumpData());
    }

    double avg(String metricName) {
        assertColumnExist(metricName);
        def values = this.getValues(metricName);
        getLogger().debug("Avg is " + values.sum()/values.size());
        return (values.sum())/(values.size());
    }

    def min(String metricName) {
        assertColumnExist(metricName);

        def values;
        if (hasColumn(metricName+" (min)"))
            values = this.getValues(metricName+" (min)");
        else
            values = this.getValues(metricName);
        getLogger().debug("Min is " + values.min());
        return values.min();
    }

    def max(String metricName) {
        assertColumnExist(metricName);

        def values;
        if (hasColumn(metricName+" (max)"))
            values = this.getValues(metricName+" (max)");
        else
            values = this.getValues(metricName);
        getLogger().debug("Max is " + values.max());
        return values.max();
    }

    def sum(String metricName) {
        assertColumnExist(metricName);

        def values;
        if (hasColumn(metricName+" (sum)"))
            values = this.getValues(metricName+" (sum)");
        else
            values = this.getValues(metricName);
        getLogger().debug("Sum is " + values.sum());
        return values.sum();
    }

    def count(String metricName) {
        assertColumnExist(metricName);

        def values = this.getValues(metricName);
        getLogger().debug("Count is " + values.size());
        return values.size();
    }

    def values(String metricName) {
        assertColumnExist(metricName);

        def values = this.getValues(metricName);
        getLogger().debug("Values are " + values);
        return values;
    }

    def values(String ... metricNames) {

        def cols = metricNames.collect { name ->
            assertColumnExist(name)
            _filteredData.getHeaderColumn(name);
        }

        //assertions hapened in line, set list of rows

        def result = _filteredData.getOrderedRows().collect() { DataRow row ->
            cols.collectEntries() { col ->
                def val;
                def d = row.getData(col);
                val = d.getTextValue();
                if (d instanceof ValueDataObject) val = (d as ValueDataObject).getValue();
                if (d instanceof  TimestampDataObject) val = (d as TimestampDataObject).getTimestampValue();

                [(col.getName()): val]
            }
        }
    }

    def first(String metricName) {
        assertColumnExist(metricName);

        def values = this.getValues(metricName);
        getLogger().debug("First value is " + values.first());
        return values.first();
    }

    def last(String metricName) {
        assertColumnExist(metricName);

        def values = this.getValues(metricName);
        getLogger().debug("Last value is " + values.last());
        return values.last();
    }

    def delta(String metricName) {
        assertColumnExist(metricName);

        def values = this.getValues(metricName);
        if(values.size()>1) {
            getLogger().debug("Last value is " + values.last() + ", second last value is " + values[-2] + ". Hence the delta is " + values.last() - values[-2]);
            return (values.last() - values[-2]).abs();
        } else {
           throw new CalculationException("There are less than two values to derive a delta from.");
            return null;
        }
    }

    def deltaMin(String metricName) {
        assertColumnExist(metricName);

        def timestampValues = this.getValueTimestamps();
        def values = this.getValues(metricName);
        if(timestampValues.size()>1) {
            def timestampDelta = timestampValues.last().getTime() - timestampValues[-2].getTime();
            if(timestampDelta == 60000) {
                return (values.last() - values[-2]).abs();
            } else if(timestampDelta > 60000){
                return (values.last() - values[-2]).abs() / (timestampDelta / 60000);
            } else {
                // the timestamp delta should actually never be less than 1 minute since this is the finest granularity where data is collected.
            }
        } else {
            throw new CalculationException("There are less than two values to derive a delta from.");
            return null;
        }
    }

    double percentage(String metricName, double percent) {
        assertColumnExist(metricName);

        def values = this.getValues(metricName);
        def baseValue;
        if(metricName.endsWith("(min)"))
            baseValue = values.min()
        else if(metricName.endsWith("(max)"))
            baseValue = values.max()
        else if(metricName.endsWith("(sum)"))
            baseValue = values.sum()
        else
            baseValue = values.sum()/values.size()
        getLogger().debug(percent + "% from " + metricName + " is " + (baseValue/100)*percent);
        return (baseValue/100)*percent;
    }

    def startTime() {
        def timestamps = this.getValueTimestamps();
        return timestamps.first();
    }

    def endTime() {
        def timestamps = this.getValueTimestamps();
        return timestamps.last();
    }

    def duration() {
        def timestamps = this.getValueTimestamps();
        return timestamps.last().getTime() - timestamps.first().getTime();
    }

    boolean hasColumn(String metricName) {
        return _filteredData.getHeaderColumn(metricName) != null;
    }

    def getValues(String metricName) throws CalculationException {
        def values = [];
        ValueColumn metricColumn = _filteredData.getHeaderColumn(metricName);
        ArrayList columnList = _filteredData._columns.getColumnsList();
        _filteredData.getOrderedRows().each { row ->
            columnList.each { column ->
                if(column.equals(metricColumn)) {
                    ValueDataObject data = row.findData(column);
                    def v = data?.value ;
                    if (v == null) v = 0;
                    values << v; // no value equals to 0
                }
            }
        }
        /*
        return _filteredData.getValues(metricColumn).collect {ValueDataObject it ->
            it.getValue();
        };
        */
        return values;
    }

    def getValueTimestamps() throws CalculationException {
        def timestamps = [];
        TimestampColumn metricColumn = _filteredData.getHeaderColumn("time");
        ArrayList columnList = _filteredData._columns.getColumnsList();
        _filteredData.getOrderedRows().each { row ->
            columnList.each { column ->
                if(column.equals(metricColumn)) {
                    TimestampDataObject timestamp = row.findData(column);
                    timestamps << timestamp.getTimestampValue();
                }
            }
        }
        return timestamps;
    }


    @Override
    Object getProperty(String property) {
        def col;
        if ( (col = _filteredData.getHeaderColumn(property))!= null) {
            if (col instanceof TextColumn) return _filteredData.getValues(col).last()?.textValue;
            if (col instanceof ValueColumn) return (_filteredData.getValues(col).last() as ValueDataObject)?.value;
            if (col instanceof TimestampColumn) return (_filteredData.getValues(col).last() as TimestampDataObject)?.timestampValue;
        }
        return super.getProperty(property)
    }

    private void assertColumnExist(String name) {
        if (!hasColumn(name)) throw new CalculationException("Column $metricName not found!");
    }
}
