package io.sslprox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class U {

	public static void async(EmptyCallback c) {
		new Thread(() -> {
			try {
				c.callback();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();
	}

	public static interface EmptyCallback {
		public void callback();
	}

	public static void swing(EmptyCallback c) {
		SwingUtilities.invokeLater(() -> c.callback());
	}

	public static <T> List<T> merge(List<T> old, List<T> updated, MatchInterface<T> m, UpdateInterface<T> u,
			RemoveInterface<T> r) {
		List<T> result = new ArrayList<T>();
		// remove missing items from updated array
		old.forEach(i -> {
			updated.forEach(i2 -> {
				if (m.match(i, i2)) {
					if (u != null)
						u.update(i2, i);
					result.add(i);
				}
			});
		});
		// add missing items
		updated.forEach(i -> {
			boolean contains = false;
			for (T i2 : result)
				if (m.match(i, i2))
					contains = true;
			if (!contains)
				result.add(i);
		});
		// remove items
		if (r != null) {
			old.forEach(o -> {
				if (!result.contains(o))
					r.remove(o);
			});
		}
		return result;
	}

	public static void exec(String cmd, Callback closeCallback, Callback outputCallback) {
		async(() -> {
			try {
				ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", cmd);
				builder.redirectErrorStream(true);
				Process p = builder.start();
				BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				while (true) {
					line = r.readLine();
					if (line == null) {
						break;
					}
					if (outputCallback != null)
						outputCallback.callback(line);
				}
				if (closeCallback != null)
					closeCallback.callback();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public static interface Callback {
		public void callback(Object... args);
	}

	public static interface MatchInterface<T> {
		public boolean match(T a, T b);
	}

	public static interface UpdateInterface<T> {
		public void update(T src, T target);
	}

	public static interface RemoveInterface<T> {
		public void remove(T remove);
	}

	public static void popup(String title, String msg) {
		JOptionPane.showMessageDialog(null, msg, title, JOptionPane.INFORMATION_MESSAGE);
	}

}
