package io.sslprox;

public class U {

	public static void async(EmptyCallback c) {
		new Thread(() -> {
			try {
				c.callback();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public static interface EmptyCallback {
		public void callback();
	}

}
