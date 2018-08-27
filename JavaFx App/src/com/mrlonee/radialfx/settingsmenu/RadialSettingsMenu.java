/**
 * Copyright 2013 (C) Mr LoNee - (Laurent NICOLAS) - www.mrlonee.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package com.mrlonee.radialfx.settingsmenu;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.win32.W32APIOptions;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.FadeTransitionBuilder;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.ImageViewBuilder;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import com.mrlonee.radialfx.core.RadialMenuItem;
import com.mrlonee.radialfx.core.RadialMenuItemBuilder;

public class RadialSettingsMenu extends Group implements SerialPortEventListener {

	private final Group itemsContainer = new Group();
	private final Color baseColor = Color.web("e0e0e0");
	private final Color hoverColor = Color.web("30c0ff");
	private final Color selectionColor = Color.BLACK;
	private final Color valueColor = Color.web("30c0ff");
	private final Color valueHoverColor = Color.web("30c0ff");

	private final double menuSize = 45;
	private final double innerRadius = 110;
	private final double radius = 200;
	private final List<RadialMenuItem> items = new ArrayList<RadialMenuItem>();
	private final DoubleProperty initialAngle = new SimpleDoubleProperty(0);
	private final RadialSettingsMenuCenter centerNode = new RadialSettingsMenuCenter();
//	SelectionEventHandler selectionEventHandler = new SelectionEventHandler();
	// SerialPortListener selectionEventHandler = new SerialPortListener();

	private RadialMenuItem selectedItem = null;
	private final Map<RadialMenuItem, List<RadialMenuItem>> itemToValues = new HashMap<RadialMenuItem, List<RadialMenuItem>>();
	private final Map<RadialMenuItem, Group> itemToGroupValue = new HashMap<RadialMenuItem, Group>();
	private final Map<RadialMenuItem, ImageView> itemAndValueToIcon = new HashMap<RadialMenuItem, ImageView>();
	private final Map<RadialMenuItem, ImageView> itemAndValueToWhiteIcon = new HashMap<RadialMenuItem, ImageView>();
	private final Map<RadialMenuItem, RadialMenuItem> valueItemToItem = new HashMap<RadialMenuItem, RadialMenuItem>();
	private final Group notSelectedItemEffect;
	private Transition openAnim;
	private int id = 0;
	private int prev_id = 1;
	private RadialMenuItem newSelectedItem = null;

	SerialPort serialPort;
	/** The port we're normally going to use. */
	private static final String PORT_NAMES[] = {
			// "/dev/tty.usbserial-A9007UX1", // Mac OS X
			// "/dev/ttyACM0", // Raspberry Pi
			// "/dev/ttyUSB0", // Linux
			"COM9" // Windows
	};
	/**
	 * A BufferedReader which will be fed by a InputStreamReader converting the
	 * bytes into characters making the displayed results codepage independent
	 */
	private BufferedReader input;
	/** The output stream to the port */
	private OutputStream output;
	/** Milliseconds to block while waiting for port open */
	private static final int TIME_OUT = 1000;
	/** Default bits per second for COM port. */
	private static final int DATA_RATE = 9600;

	public void initialize() {
		// the next line is for Raspberry Pi and
		// gets us into the while loop and was suggested here was suggested
		// http://www.raspberrypi.org/phpBB3/viewtopic.php?f=81&t=32186
		// System.setProperty("gnu.io.rxtx.SerialPorts", "/dev/ttyACM0");

		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

		// First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			for (String portName : PORT_NAMES) {
				if (currPortId.getName().equals(portName)) {
					portId = currPortId;
					break;
				}
			}
		}
		if (portId == null) {
			System.out.println("Could not find COM port.");
			return;
		}

		try {
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);

			// set port parameters
			serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_7, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);

			// open the streams
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			output = serialPort.getOutputStream();

			// add event listeners
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	/**
	 * This should be called when you stop using the port. This will prevent port
	 * locking on platforms like Linux.
	 */
	public synchronized void close() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

	/**
	 * Handle an event on the serial port. Read the data and print it.
	 */
	public synchronized void serialEvent(SerialPortEvent oEvent) {
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			try {
				if (input.ready() == true) {
					String inputLine = input.readLine();
					System.out.println(inputLine);

					id = Integer.parseInt(inputLine);

					choose_from_wheel(inputLine);
					// prev_id =Integer.parseInt(inputLine);
					// Thread.sleep(50);

				}

			} catch (Exception e) {
				System.err.println(e.toString());
			}
		}
		// Ignore all the other eventTypes, but you should consider the other ones.
	}

	private void choose_from_wheel(String input) {
		// id = Integer.parseInt(input);
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (id != prev_id) {

					System.out.println("id : " + id);
					System.out.println("prev: " + prev_id);
					if (id < 9) {
						try {
							open_wheel();
						} catch (IOException e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
						System.out.println(items.get(id));
						items.get(id).HoverEnter();
						centerNode.displayCenter(items.get(id));
//				
						items.get(prev_id).HoverExit();
						centerNode.hideCenter(items.get(prev_id));
//					
						// items.get(id).redraw();
						// items.get(prev_id).redraw();

						prev_id = id;
					} else if (id >= 90 && id <= 97) {
						id = id - 90;
						System.out.println("pressedd");
						newSelectedItem = (RadialMenuItem) items.get(id);
						System.out.println(items.get(id));
						if (selectedItem == newSelectedItem) {

							// closeValueSelection(newSelectedItem);
							// centerNode.hideCenter(items.get(id));

						} else {
							// openValueSelection(newSelectedItem);
							try {
							
								//openZbrush();
								openPhotoshop();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							Robot r = null;
							try {
								r = new Robot();
								// Runtime runtime = Runtime.getRuntime();

								// runtime.exec( "C:\\Program Files
								// (x86)\\Google\\Chrome\\Application\\chrome.exe" );
							} catch (AWTException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();

								// TODO Auto-generated catch block
							}

							// r.mouseMove(500, 500);
							// r.mousePress(InputEvent.BUTTON1_MASK);
							// r.mouseRelease(InputEvent.BUTTON1_MASK);

							// ********* Photoshop ************************///
							switch (id) {
							case 0: // Transform tool
								r.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
								r.keyPress(java.awt.event.KeyEvent.VK_T);

								// centerNode.displayCenter(items.get(id));
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								r.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
								r.keyRelease(java.awt.event.KeyEvent.VK_T);
								break;
							case 1: // Select Rect Marquee tool
								r.keyPress(java.awt.event.KeyEvent.VK_M);

								// centerNode.displayCenter(items.get(id));
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								r.keyRelease(java.awt.event.KeyEvent.VK_M);
								break;
							case 2: // color picker
								r.keyPress(java.awt.event.KeyEvent.VK_I);

								// centerNode.displayCenter(items.get(id));
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								r.keyRelease(java.awt.event.KeyEvent.VK_I);
								break;
							case 3: // brush
								r.keyPress(java.awt.event.KeyEvent.VK_B);

								// centerNode.displayCenter(items.get(id));
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								r.keyRelease(java.awt.event.KeyEvent.VK_B);
								break;
							case 4: // Eraser
								r.keyPress(java.awt.event.KeyEvent.VK_E);
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								r.keyRelease(java.awt.event.KeyEvent.VK_E);
								break;
							case 5: // Text tool
								r.keyPress(java.awt.event.KeyEvent.VK_T);
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								r.keyRelease(java.awt.event.KeyEvent.VK_T);
								break;
							case 6: // move tool
								r.keyPress(java.awt.event.KeyEvent.VK_V);

								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								r.keyRelease(java.awt.event.KeyEvent.VK_V);
								break;
							case 7: // hand tool
								r.keyPress(java.awt.event.KeyEvent.VK_H);


								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {

									e.printStackTrace();
								}
								r.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
								r.keyRelease(java.awt.event.KeyEvent.VK_H);
								break;
							}
							
							// *******************************************************////

							// ********* ZBRUSH ************************///
/*
							switch (id) {
							case 0: // move tool
								r.keyPress(java.awt.event.KeyEvent.VK_W);
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								r.keyRelease(java.awt.event.KeyEvent.VK_W);
								break;
							case 1: // scale tool
								r.keyPress(java.awt.event.KeyEvent.VK_E);
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {

									e.printStackTrace();
								}
								r.keyRelease(java.awt.event.KeyEvent.VK_E);
								break;
							case 2: // Rotate tool
								r.keyPress(java.awt.event.KeyEvent.VK_R);
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								r.keyRelease(java.awt.event.KeyEvent.VK_R);
								break;
							case 3: // Standard brush
								try {
									r.keyPress(java.awt.event.KeyEvent.VK_B);

									Thread.sleep(50);
									r.keyRelease(java.awt.event.KeyEvent.VK_B);
									Thread.sleep(50);

									r.keyPress(java.awt.event.KeyEvent.VK_S);
									Thread.sleep(50);
									r.keyRelease(java.awt.event.KeyEvent.VK_S);
									Thread.sleep(50);

									r.keyPress(java.awt.event.KeyEvent.VK_T);
									Thread.sleep(50);
									r.keyRelease(java.awt.event.KeyEvent.VK_T);


									
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								break;
							case 4: // Move brush
								try {
									r.keyPress(java.awt.event.KeyEvent.VK_B);

									Thread.sleep(50);
									r.keyRelease(java.awt.event.KeyEvent.VK_B);
									Thread.sleep(50);

									r.keyPress(java.awt.event.KeyEvent.VK_M);
									Thread.sleep(50);
									r.keyRelease(java.awt.event.KeyEvent.VK_M);
									Thread.sleep(50);

									r.keyPress(java.awt.event.KeyEvent.VK_V);
									Thread.sleep(50);
									r.keyRelease(java.awt.event.KeyEvent.VK_V);


									
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								break;
							case 5: // snake hook brush
								try {
									r.keyPress(java.awt.event.KeyEvent.VK_B);

									Thread.sleep(50);
									r.keyRelease(java.awt.event.KeyEvent.VK_B);
									Thread.sleep(50);

									r.keyPress(java.awt.event.KeyEvent.VK_S);
									Thread.sleep(50);
									r.keyRelease(java.awt.event.KeyEvent.VK_S);
									Thread.sleep(50);

									r.keyPress(java.awt.event.KeyEvent.VK_H);
									Thread.sleep(50);
									r.keyRelease(java.awt.event.KeyEvent.VK_H);


									
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								break;
							case 6: // clay buildup brush
								try {
									r.keyPress(java.awt.event.KeyEvent.VK_B);

									Thread.sleep(50);
									r.keyRelease(java.awt.event.KeyEvent.VK_B);
									Thread.sleep(50);

									r.keyPress(java.awt.event.KeyEvent.VK_C);
									Thread.sleep(50);
									r.keyRelease(java.awt.event.KeyEvent.VK_C);
									Thread.sleep(50);

									r.keyPress(java.awt.event.KeyEvent.VK_B);
									Thread.sleep(50);
									r.keyRelease(java.awt.event.KeyEvent.VK_B);


									
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								break;
							case 7: // Symmetry Toggle
								r.keyPress(java.awt.event.KeyEvent.VK_X);
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								r.keyRelease(java.awt.event.KeyEvent.VK_X);
								break;
							}
*/
							// ********************************************////
						}
					}
				}

			}
		});

	}

	/*
	 * public class SelectionEventHandler implements EventHandler<SerialPortEvent> {
	 * 
	 * 
	 * }
	 */

	public RadialSettingsMenu() {

		initialAngle.addListener(new ChangeListener<Number>() {
			@Override
			public void changed(final ObservableValue<? extends Number> paramObservableValue, final Number paramT1,
					final Number paramT2) {
				RadialSettingsMenu.this.setInitialAngle(paramObservableValue.getValue().doubleValue());
			}
		});
		centerNode.visibleProperty().bind(visibleProperty());
		getChildren().add(itemsContainer);
		getChildren().add(centerNode);

		addMenuItem("resources/icons/gemicon/PNG/32x32/row 1/1.png");
		addMenuItem("resources/icons/gemicon/PNG/32x32/row 1/2.png");
		addMenuItem("resources/icons/gemicon/PNG/32x32/row 1/3.png");
		addMenuItem("resources/icons/gemicon/PNG/32x32/row 1/4.png");
		addMenuItem("resources/icons/gemicon/PNG/32x32/row 1/5.png");
		addMenuItem("resources/icons/gemicon/PNG/32x32/row 1/6.png");
		addMenuItem("resources/icons/gemicon/PNG/32x32/row 1/7.png");
		addMenuItem("resources/icons/gemicon/PNG/32x32/row 1/8.png");

		final RadialMenuItem notSelected1 = createNotSelectedItemEffect();
		final RadialMenuItem notSelected2 = createNotSelectedItemEffect();
		notSelected2.setClockwise(false);

		notSelectedItemEffect = new Group(notSelected1, notSelected2);
		notSelectedItemEffect.setVisible(false);
		notSelectedItemEffect.setOpacity(0);

		itemsContainer.getChildren().add(notSelectedItemEffect);

		computeItemsStartAngle();

		setTranslateX(210);
		setTranslateY(210);
		this.initialize();
		Thread t = new Thread() {
			public void run() {
				// the following line will keep this app alive for 1000 seconds,
				// waiting for events to occur and responding to them (printing incoming
				// messages to console).
				try {
					Thread.sleep(1000000);
				} catch (InterruptedException ie) {
				}
			}
		};
		t.start();
		System.out.println("Started");
	}

	private RadialMenuItem createNotSelectedItemEffect() {
		final RadialMenuItem notSelectedItemEffect = RadialMenuItemBuilder.create().length(180)
				.backgroundFill(baseColor).startAngle(0).strokeFill(baseColor).backgroundMouseOnFill(baseColor)
				.strokeMouseOnFill(baseColor).innerRadius(innerRadius).radius(radius).offset(0).clockwise(true)
				.strokeVisible(true).backgroundVisible(true).build();
		return notSelectedItemEffect;
	}

	private void addMenuItem(final String iconPath) {
		final ImageView imageView = getImageView(iconPath);
		final ImageView centerView = getImageView(iconPath.replace("32x32", "64x64"));
		final ImageView value1View = getImageView(iconPath.replace("row 1", "row 1"));
		final ImageView value2View = getImageView(iconPath.replace("row 1", "row 1"));
		final ImageView imageViewWhite = getImageView(iconPath);
		final RadialMenuItem item = newRadialMenuItem(imageView, imageViewWhite);
		final RadialMenuItem value1Item = newValueRadialMenuItem(value1View);
		final RadialMenuItem value2Item = newValueRadialMenuItem(value2View);

		valueItemToItem.put(value1Item, item);
		valueItemToItem.put(value2Item, item);
		List<RadialMenuItem> values;
		Group valueGroup;
		if (Math.random() < 0.5) {
			final ImageView value3View = getImageView(iconPath.replace("row 1", "row 1"));
			final RadialMenuItem value3Item = newValueRadialMenuItem(value3View);
			valueItemToItem.put(value3Item, item);
			values = Arrays.asList(value1Item, value2Item, value3Item);
			valueGroup = new Group(value1Item, value2Item, value3Item);
		} else {
			values = Arrays.asList(value1Item, value2Item);
			valueGroup = new Group(value1Item, value2Item);
		}

		itemToValues.put(item, values);
		itemToGroupValue.put(item, valueGroup);
		valueGroup.setVisible(false);

		itemsContainer.getChildren().addAll(item, valueGroup);
		// item.addEventHandler(KeyEvent.KEY_PRESSED, selectionEventHandler);
		// item.addEventHandler(SerialPortEvent.DATA_AVAILABLE, selectionEventHandler);

		centerNode.addCenterItem(item, centerView);

	}

	private RadialMenuItem newValueRadialMenuItem(final ImageView imageView) {
		final RadialMenuItem item = RadialMenuItemBuilder.create().length(menuSize).backgroundFill(valueColor)
				.strokeFill(valueColor).backgroundMouseOnFill(valueHoverColor).strokeMouseOnFill(valueHoverColor)
				.innerRadius(innerRadius).radius(radius).offset(0).clockwise(true).graphic(imageView)
				.backgroundVisible(true).strokeVisible(true).build();

		item.addEventHandler(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {

			@Override
			public void handle(final KeyEvent event) {
				if (event.getCode() == KeyCode.UP) {
					System.out.println(id + " id is now");
				}
				final RadialMenuItem valuItem = (RadialMenuItem) items.get(id);
				final RadialMenuItem item = valueItemToItem.get(valuItem);
				RadialSettingsMenu.this.closeValueSelection(item);

			}

		});
		itemAndValueToIcon.put(item, imageView);
		return item;
	}

	private RadialMenuItem newRadialMenuItem(final ImageView imageView, final ImageView imageViewWhite) {
		final RadialMenuItem item = RadialMenuItemBuilder.create().backgroundFill(baseColor).strokeFill(baseColor)
				.backgroundMouseOnFill(hoverColor).strokeMouseOnFill(hoverColor).radius(radius).innerRadius(innerRadius)
				.length(menuSize).clockwise(true).backgroundVisible(true).strokeVisible(true).offset(0).build();

		if (imageViewWhite != null) {
			item.setGraphic(new Group(imageView, imageViewWhite));
			imageViewWhite.setOpacity(0.0);
		} else {
			item.setGraphic(new Group(imageView));
		}
		items.add(item);
		itemAndValueToIcon.put(item, imageView);
		itemAndValueToWhiteIcon.put(item, imageViewWhite);
		return item;
	}

	private void computeItemsStartAngle() {
		double angleOffset = initialAngle.get();
		for (final RadialMenuItem item : items) {
			item.setStartAngle(angleOffset);
			angleOffset = angleOffset + item.getLength();
		}
	}

	private void setInitialAngle(final double angle) {
		initialAngle.set(angle);
		computeItemsStartAngle();
	}

	ImageView getImageView(final String path) {
		ImageView imageView = null;
		try {
			imageView = ImageViewBuilder.create().image(new Image(new FileInputStream(path))).build();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}
		assert (imageView != null);
		return imageView;

	}

	private void openValueSelection(final RadialMenuItem newSelectedItem) {
		selectedItem = newSelectedItem;

		notSelectedItemEffect.toFront();

		itemToGroupValue.get(selectedItem).setVisible(true);
		itemToGroupValue.get(selectedItem).toFront();
		selectedItem.toFront();

		openAnim = createOpenAnimation(selectedItem);
		openAnim.play();

	}

	private void closeValueSelection(final RadialMenuItem newSelectedItem) {
		openAnim.setAutoReverse(true);
		openAnim.setCycleCount(2);
		openAnim.setOnFinished(new EventHandler<ActionEvent>() {

			@Override
			public void handle(final ActionEvent event) {
				newSelectedItem.setBackgroundFill(baseColor);
				newSelectedItem.setStrokeFill(baseColor);
				newSelectedItem.setBackgroundMouseOnFill(hoverColor);
				newSelectedItem.setStrokeMouseOnFill(hoverColor);
				notSelectedItemEffect.setVisible(false);
				itemToGroupValue.get(newSelectedItem).setVisible(false);
			}

		});
		openAnim.playFrom(Duration.millis(400));
		selectedItem = null;

	}

	private void openPhotoshop() throws IOException {
		final User32 user32 = User32.INSTANCE;
		user32.EnumWindows(new WNDENUMPROC() {
			int count = 0;

			public boolean callback(HWND hWnd, Pointer arg1) {
				char[] windowText = new char[512];
				user32.GetWindowText(hWnd, windowText, 512);
				String wText = Native.toString(windowText);
				// RECT rectangle = new RECT();
				// user32.GetWindowRect(hWnd, rectangle);

				// returns only Photoshop windows
				if (wText.isEmpty() || (!wText.contains("@") && !wText.contains("%"))) {
					return true;
				}
				System.out.println("Found window with text " + hWnd + ", total " + ++count + " Text: " + wText);

				user32.ShowWindow(hWnd, User32.SW_RESTORE);
				user32.ShowWindow(hWnd, User32.SW_NORMAL);
				user32.SetForegroundWindow(hWnd);
				// user32.SetFocus(hWnd);
				return true;
			}
		}, null);
	}

	private void openZbrush() throws IOException {
		final User32 user32 = User32.INSTANCE;
		user32.EnumWindows(new WNDENUMPROC() {
			int count = 0;

			public boolean callback(HWND hWnd, Pointer arg1) {
				char[] windowText = new char[512];
				user32.GetWindowText(hWnd, windowText, 512);
				String wText = Native.toString(windowText);
				// RECT rectangle = new RECT();
				// user32.GetWindowRect(hWnd, rectangle);

				// returns only Photoshop windows
				if (wText.isEmpty() || (!wText.contains("ZBrush"))) {
					return true;
				}
				System.out.println("Found window with text " + hWnd + ", total " + ++count + " Text: " + wText);

				//user32.ShowWindow(hWnd, User32.SW_RESTORE);
				//user32.ShowWindow(hWnd, User32.SW_NORMAL);
				user32.SetForegroundWindow(hWnd);
				// user32.SetFocus(hWnd);
				return true;
			}
		}, null);
	}

	private void open_wheel() throws IOException {
		final User32 user32 = User32.INSTANCE;
		user32.EnumWindows(new WNDENUMPROC() {
			int count = 0;

			public boolean callback(HWND hWnd, Pointer arg1) {
				char[] windowText = new char[512];
				user32.GetWindowText(hWnd, windowText, 512);
				String wText = Native.toString(windowText);
				// RECT rectangle = new RECT();
				// user32.GetWindowRect(hWnd, rectangle);

				// returns only Photoshop windows
				if (wText.isEmpty() || (!wText.contains("Radial"))) {
					return true;
				}
				System.out.println("Found window with text " + hWnd + ", total " + ++count + " Text: " + wText);

				// user32.ShowWindow(hWnd, User32.SW_SHOW);
				// user32.ShowWindow(hWnd, User32.SW_NORMAL);
				user32.SetForegroundWindow(hWnd);
				user32.SetFocus(hWnd);
				// user32.SW
				return true;
			}
		}, null);
	}

	private Transition createOpenAnimation(final RadialMenuItem newSelectedItem) {

		// Children slide animation
		final List<RadialMenuItem> children = itemToValues.get(newSelectedItem);

		double startAngleEnd = 0;
		final double startAngleBegin = newSelectedItem.getStartAngle();
		final ParallelTransition transition = new ParallelTransition();

		itemToGroupValue.get(newSelectedItem).setVisible(true);
		int internalCounter = 1;
		for (int i = 0; i < children.size(); i++) {
			final RadialMenuItem it = children.get(i);
			if (i % 2 == 0) {
				startAngleEnd = startAngleBegin + internalCounter * it.getLength();
			} else {
				startAngleEnd = startAngleBegin - internalCounter * it.getLength();
				internalCounter++;
			}

			final Animation itemTransition = new Timeline(
					new KeyFrame(Duration.ZERO, new KeyValue(it.startAngleProperty(), startAngleBegin)),
					new KeyFrame(Duration.millis(400), new KeyValue(it.startAngleProperty(), startAngleEnd)));

			transition.getChildren().add(itemTransition);

			final ImageView image = itemAndValueToIcon.get(it);
			image.setOpacity(0.0);
			final Timeline iconTransition = new Timeline(
					new KeyFrame(Duration.millis(0), new KeyValue(image.opacityProperty(), 0)),
					new KeyFrame(Duration.millis(300), new KeyValue(image.opacityProperty(), 0)),
					new KeyFrame(Duration.millis(400), new KeyValue(image.opacityProperty(), 1.0)));

			transition.getChildren().add(iconTransition);
		}

		// Selected item background color change
		final DoubleProperty backgroundColorAnimValue = new SimpleDoubleProperty();
		final ChangeListener<? super Number> listener = new ChangeListener<Number>() {

			@Override
			public void changed(final ObservableValue<? extends Number> arg0, final Number arg1, final Number arg2) {
				final Color c = hoverColor.interpolate(selectionColor, arg2.floatValue());

				newSelectedItem.setBackgroundFill(c);
				newSelectedItem.setStrokeFill(c);
				newSelectedItem.setBackgroundMouseOnFill(c);
				newSelectedItem.setStrokeMouseOnFill(c);
			}
		};

		backgroundColorAnimValue.addListener(listener);

		final Animation itemTransition = new Timeline(
				new KeyFrame(Duration.ZERO, new KeyValue(backgroundColorAnimValue, 0)),
				new KeyFrame(Duration.millis(300), new KeyValue(backgroundColorAnimValue, 1.0)));
		transition.getChildren().add(itemTransition);

		// Selected item image icon color change
		final FadeTransition selectedItemImageBlackFade = FadeTransitionBuilder.create()
				.node(itemAndValueToIcon.get(newSelectedItem)).duration(Duration.millis(400)).fromValue(1.0)
				.toValue(0.0).build();

		final FadeTransition selectedItemImageWhiteFade = FadeTransitionBuilder.create()
				.node(itemAndValueToWhiteIcon.get(newSelectedItem)).duration(Duration.millis(400)).fromValue(0)
				.toValue(1.0).build();
		transition.getChildren().addAll(selectedItemImageBlackFade, selectedItemImageWhiteFade);

		// Unselected items fading
		final FadeTransition notSelectedTransition = FadeTransitionBuilder.create().node(notSelectedItemEffect)
				.duration(Duration.millis(200)).delay(Duration.millis(200)).fromValue(0).toValue(0.8).build();
		notSelectedItemEffect.setOpacity(0);
		notSelectedItemEffect.setVisible(true);

		transition.getChildren().add(notSelectedTransition);
		return transition;
	}
	/*
	 * public interface User32 extends W32APIOptions {
	 * 
	 * User32 instance = (User32) Native.loadLibrary("user32", User32.class,
	 * DEFAULT_OPTIONS);
	 * 
	 * 
	 * boolean ShowWindow(HWND hWnd, int nCmdShow);
	 * 
	 * boolean SetForegroundWindow(HWND hWnd);
	 * 
	 * HWND EnumWindows(String winClass, String title);
	 * 
	 * int SW_SHOW = 1;
	 * 
	 * }
	 * 
	 */

}