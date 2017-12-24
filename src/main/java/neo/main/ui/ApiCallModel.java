package neo.main.ui;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.util.MapUtil;
import neo.network.model.LocalNodeData;
import neo.network.model.RemoteNodeData;

/**
 * this class represents the model for the stats page.
 *
 * @author coranos
 *
 */
public final class ApiCallModel extends AbstractRefreshingModel {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ApiCallModel.class);

	private static final long serialVersionUID = 1L;

	/**
	 * list of the names of the statistics.
	 */
	private final List<String> statsNameList = new ArrayList<>();

	/**
	 * lists of the values of the statistics, converted from numbers to strings.
	 */
	private final List<String> statsValueList = new ArrayList<>();

	/**
	 * add stats about what API calls were made.
	 */
	private void addApiCallStats() {
		final Map<String, Long> apiCallMap = new TreeMap<>();
		MapUtil.increment(apiCallMap, LocalNodeData.API_CALL_MAP);

		for (final String key : apiCallMap.keySet()) {
			final long value = apiCallMap.get(key);
			addNameAndValue(key, value);
		}
	}

	/**
	 * adds the name and value to the stats list, formatting the value as an
	 * integer.
	 *
	 * @param name
	 *            the name of the statistic.
	 *
	 * @param value
	 *            the value of the statistic.
	 */
	private void addNameAndValue(final String name, final long value) {
		statsNameList.add(name);
		statsValueList.add(NumberFormat.getIntegerInstance().format(value));
	}

	@Override
	public int getColumnCount() {
		synchronized (ApiCallModel.this) {
			return 2;
		}
	}

	@Override
	public String getColumnName(final int columnIndex) {
		synchronized (ApiCallModel.this) {
			switch (columnIndex) {
			case 0:
				return "Name";
			case 1:
				return "Value";
			}
		}
		throw new RuntimeException("unknown column name index:" + columnIndex);
	}

	/**
	 * return the duration in seconds.
	 *
	 * @param localNodeData
	 *            the local node data to use.
	 * @return the duration in seconds.
	 */
	public long getDurationInSeconds(final LocalNodeData localNodeData) {
		return (System.currentTimeMillis() - localNodeData.getStartTime()) / 1000;
	}

	@Override
	public int getRowCount() {
		synchronized (ApiCallModel.this) {
			return statsNameList.size();
		}
	}

	@Override
	public String getThreadName() {
		return "ApiCallModel.Refresh";
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex) {
		synchronized (ApiCallModel.this) {
			switch (columnIndex) {
			case 0:
				return statsNameList.get(rowIndex);
			case 1:
				return statsValueList.get(rowIndex);
			}
		}
		throw new RuntimeException("unknown column value index:" + columnIndex);
	}

	@Override
	public void nodeDataChanged(final LocalNodeData localNodeData, final Set<RemoteNodeData> peerDataSet) {
		LOG.trace("STARTED peersChanged count:{}", peerDataSet.size());
		synchronized (localNodeData) {
			synchronized (ApiCallModel.this) {
				statsNameList.clear();
				statsValueList.clear();

				addApiCallStats();

				try (FileOutputStream fout = new FileOutputStream("ApiCallModel.txt");
						PrintWriter pw = new PrintWriter(fout, true)) {
					for (int columnIndex = 0; columnIndex < getColumnCount(); columnIndex++) {
						pw.print(getColumnName(columnIndex));
						pw.print("\t");
					}
					pw.println();
					for (int rowIndex = 0; rowIndex < getRowCount(); rowIndex++) {
						for (int columnIndex = 0; columnIndex < getColumnCount(); columnIndex++) {
							pw.print(getValueAt(rowIndex, columnIndex));
							pw.print("\t");
						}
						pw.println();
					}
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}

				setRefresh(true);
			}
		}
		LOG.trace("SUCCESS peersChanged");
	}
}
