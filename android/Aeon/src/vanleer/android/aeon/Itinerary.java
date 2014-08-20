package vanleer.android.aeon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemLongClickListener;

// Something for sending destinations to Navigator
// I/ActivityManager( 118): Starting activity: Intent { act=android.intent.action.VIEW dat=google.navigation:///?q=Some%20place cmp=brut.googlemaps/com.google.android.maps.driveabout.app.NavigationActivity }

public final class Itinerary extends Activity implements OnClickListener {

	private final int listViewId = R.id.listView_itinerary;
	private ListView itineraryListView;
	private ArrayList<ItineraryItem> itineraryItemList;
	private ItineraryItemAdapter itineraryItems;
	private final boolean loggedIntoGoogle = /* false */true; // for debugging
	private LocationManager locationManager;
	private Location currentLocation = null;
	private static final int GET_NEW_DESTINATION = 0;
	private static final int ADD_DESTINATION = 1;
	private static final int UPDATE_DESTINATION = 2;
	private ProgressDialog waitSpinner;
	private boolean waitingForGps = false;
	private ItineraryItem origin = null;
	private ItineraryItem addNewItemItem = null;
	private int selectedItemPosition = -1;
	private Geocoder theGeocoder = null;
	private boolean travelling = false;
	private int currentDestinationIndex = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.itinerary);

		// FOR TESTING
		// currentLocation = new Location("test");
		// currentLocation.setLatitude(38.477548);
		// currentLocation.setLongitude(-91.051562);
		// FOR TESTING

		itineraryItemList = new ArrayList<ItineraryItem>();
		itineraryItems = new ItineraryItemAdapter(this, R.layout.itinerary_item, itineraryItemList);
		itineraryListView = (ListView) findViewById(listViewId);
		itineraryListView.setAdapter(itineraryItems);

		// TODO: Use this to verify location service is available
		// GooglePlayServicesUtil.isGooglePlayServicesAvailable();

		theGeocoder = new Geocoder(this);

		// Acquire a reference to the system Location Manager
		locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		LocationListener locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location provider.
				onNewLocation(location);
			}

			public void onStatusChanged(String provider, int status, Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};

		// Register the listener with the Location Manager to receive location updates
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

		configureItineraryListViewLongClickListener();
		initializeOrigin();
		initializeAddNewItineraryItem();
	}

	private void initializeAddNewItineraryItem() {
		addNewItemItem = new ItineraryItem(getString(R.string.add_destination_itinerary_item));
		LayoutInflater vi = (LayoutInflater) this.getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = vi.inflate(R.layout.itinerary_item, null);

		// TextView destinationName = (TextView) v.findViewById(R.id.textView_destinationName);
		TextView arrivalVicinity = (TextView) v.findViewById(R.id.textView_arrivalLocation);
		TextView arrivalTime = (TextView) v.findViewById(R.id.textView_arrivalTime);
		TextView travelDistance = (TextView) v.findViewById(R.id.textView_travelDistance);
		TextView travelTime = (TextView) v.findViewById(R.id.textView_travelTime);
		TextView stayDuration = (TextView) v.findViewById(R.id.textView_stayDuration);
		TextView departureVicinity = (TextView) v.findViewById(R.id.textView_departureLocation);
		TextView departureTime = (TextView) v.findViewById(R.id.textView_departureTime);

		arrivalVicinity.setVisibility(View.GONE);
		arrivalTime.setVisibility(View.GONE);
		travelDistance.setVisibility(View.GONE);
		travelTime.setVisibility(View.GONE);
		stayDuration.setVisibility(View.GONE);
		departureVicinity.setVisibility(View.GONE);
		departureTime.setVisibility(View.GONE);

		// TODO: define custom color scheme for this item
		// destinationName.setTextColor(Color.BLACK);
		// destinationName.setBackgroundColor(Color.WHITE);

		itineraryItemList.add(addNewItemItem);
		itineraryItems.add(itineraryItemList.get(itineraryItemList.size() - 1));
	}

	class ItineraryUpdater implements Runnable {
		public void run() {
			if (origin.atLocation()) {
				updateTimes();
			}
		}
	}

	class DepartureReminder implements Runnable {
		private ItineraryItem origin = null;
		private ItineraryItem destination = null;

		public DepartureReminder(ItineraryItem origin, ItineraryItem destination) {
			this.origin = origin;
			this.destination = destination;
		}

		public void run() {
			String message = "Depart from " + origin.getName() + " and head to " + destination.getName() + " at " + origin.getSchedule().getDepartureTimeString();

			Builder timeToGoAlert = new AlertDialog.Builder(Itinerary.this);
			timeToGoAlert.setMessage(message);
			timeToGoAlert.setTitle("Reminder");
			timeToGoAlert.setPositiveButton("ok", null); // CharSequence[] items = { "5", "10", "15", "30" };//timeToGo.setSingleChoiceItems(items, 0, null);
			timeToGoAlert.show();

			NotificationCompat.Builder timeToGoNotiBuilder = new NotificationCompat.Builder(Itinerary.this);
			timeToGoNotiBuilder.setContentTitle("Time to leave");
			String notiMessage = "Depart from " + origin.getName() + " and head to " + destination.getName();
			timeToGoNotiBuilder.setContentText(notiMessage);
			timeToGoNotiBuilder.setWhen(origin.getSchedule().getDepartureTime().getTime());
			timeToGoNotiBuilder.setContentInfo(destination.getFormattedDistance());
			timeToGoNotiBuilder.setSmallIcon(R.drawable.arrive_notification);
			timeToGoNotiBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

			// TODO: Display either alert or notification not both
			// TODO: Return to itinerary activity when notification touched
			// TODO: Manage frequency of notifications/alerts

			Intent result = new Intent(Itinerary.this, Itinerary.class);
			// TaskStackBuilder stackBuilder = TaskStackBuilder.create(Itinerary.this);
			// stackBuilder.addParentStack(Itinerary.class);
			// stackBuilder.addNextIntent(result);
			// PendingIntent pendingResult = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
			PendingIntent pendingResult = PendingIntent.getActivity(Itinerary.this, 0, result, PendingIntent.FLAG_UPDATE_CURRENT);

			timeToGoNotiBuilder.setContentIntent(pendingResult);

			// timeToGoNotiBuilder.setLargeIcon(R.drawable.arrive_launcher);

			NotificationManager notiMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			// TODO: make enum for notification IDs
			notiMgr.notify(0, timeToGoNotiBuilder.build());
		}
	}

	private void configureItineraryListViewLongClickListener() {
		itineraryListView.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				if (position == (itineraryItemList.size() - 1)) {
					startSearchActivity();
				} else {
					selectedItemPosition = position;
					updateArrivalDepartureTimes(itineraryItemList.get(position));
				}
				return true;
			}
		});
	}

	private void waitForGps() {
		waitSpinner = ProgressDialog.show(Itinerary.this, "", "waiting for location...", true);
		waitingForGps = true;
		new Thread() {
			@Override
			public void run() {
				while (currentLocation == null) {
					try {
						sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				waitSpinner.dismiss();
			}
		}.start();
	}

	protected void onNewLocation(Location location) {
		currentLocation = location;
		if (origin.getLocation() == null || itineraryItemList.size() <= 2) {
			currentDestinationIndex = 0;
			updateOrigin();
		}

		updateTravelStatus();

		if (waitingForGps) {
			waitingForGps = false;
		}
	}

	private void updateTravelStatus() {
		if (travelling) {
			travelling = !haveArrived();
			if (!travelling) {
				itineraryItemList.get(currentDestinationIndex).setAtLocation();
				itineraryItems.notifyDataSetChanged();
			}
		} else {
			travelling = haveDeparted();
			if (travelling) {
				itineraryItemList.get(currentDestinationIndex).setLocationExpired();
				++currentDestinationIndex;
				if (currentDestinationIndex > (itineraryItemList.size() - 2)) currentDestinationIndex = itineraryItemList.size() - 2;
				itineraryItemList.get(currentDestinationIndex).setEnRoute();
				itineraryItems.notifyDataSetChanged();
				// TODO: Display map
			}
		}
	}

	private boolean haveDeparted() {
		return !travelling && (currentLocation.distanceTo(itineraryItemList.get(currentDestinationIndex).getLocation()) > 100);
	}

	private boolean haveArrived() {
		return travelling && (currentLocation.distanceTo(itineraryItemList.get(currentDestinationIndex).getLocation()) < 50);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.itinerary_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.submenu_item_add_destination_google_search:
			startSearchActivity();
			break;
		case R.id.submenu_item_add_destination_my_location:
			GetMyLocationInfo();
			break;
		case R.id.submenu_item_add_destination_starred_locations:
			if (loggedIntoGoogle) {
				// TODO: Something
			} else {
				// TODO: Something else
			}
			break;
		case R.id.menu_item_call_destination:
			break;
		case R.id.submenu_item_clear_itinerary_yes:
			itineraryItems.clear();
			itineraryItemList.clear();
			initializeOrigin();
			initializeAddNewItineraryItem();
			break;
		case R.id.menu_item_clear_itinerary:
		case R.id.submenu_item_clear_itinerary_no:
			break;
		case R.id.menu_item_edit_destination:
			break;
		case R.id.menu_item_move_destination:
			break;
		case R.id.menu_item_save_itinerary:
			break;
		default:
			break;
		}
		return true;
	}

	private void startSearchActivity() {
		Intent startItineraryOpen = new Intent(Itinerary.this, PlacesSearchActivity.class);

		if (itineraryItemList.isEmpty()) {
			startItineraryOpen.putExtra("location", currentLocation);
		} else {
			ItineraryItem lastDestination = getFinalDestination();
			startItineraryOpen.putExtra("location", lastDestination.getLocation());
		}

		startActivityForResult(startItineraryOpen, GET_NEW_DESTINATION);
	}

	private void updateOrigin() {
		origin.getSchedule().setDepartureTime(new Date());
		if (currentLocation != null) {
			try {
				origin.updateLocation(currentLocation, getLocationAddress(currentLocation));
			} catch (NullPointerException e) {
				// TODO Location was null
			}
		}
		itineraryItems.notifyDataSetChanged();
	}

	void updateTimes() {
		if (origin.getSchedule().getDepartureTime().before(new Date())) {
			origin.getSchedule().setDepartureTime(new Date());
		}

		for (int i = 1; i < (itineraryItemList.size() - 1); ++i) {
			itineraryItemList.get(i).updateSchedule(itineraryItemList.get(i - 1).getSchedule().getDepartureTime());
		}

		itineraryItems.notifyDataSetChanged();
	}

	private void initializeOrigin() {
		origin = new ItineraryItem("My location (locating...)");
		origin.setAtLocation();
		Schedule departNow = new Schedule();
		departNow.setDepartureTime(new Date());
		origin.setSchedule(departNow);
		if (currentLocation != null) {
			try {
				origin.updateLocation(currentLocation, getLocationAddress(currentLocation));
			} catch (NullPointerException e) {
				// TODO Location was null
			}
		}
		itineraryItemList.add(0, origin);
		itineraryItems.insert(itineraryItemList.get(0), 0);

		new Thread() {
			@Override
			public void run() {
				// TODO: Change nextMinute to current location departure time plus one minute

				while (true) {
					Calendar now = Calendar.getInstance();
					now.setTime(new Date());

					Calendar nextMinute = Calendar.getInstance();
					nextMinute.setTime(now.getTime());
					nextMinute.add(Calendar.MINUTE, 1);
					nextMinute.set(Calendar.SECOND, 0);

					if (now.getTime().before(nextMinute.getTime())) {
						try {
							sleep(nextMinute.getTimeInMillis() - now.getTimeInMillis());
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					doStuff();
				}
			}

			private void doStuff() {
				ItineraryItem currentlyAt = itineraryItemList.get(currentDestinationIndex);

				if (currentlyAt.getSchedule().getDepartureTime().before(new Date())) {
					Itinerary.this.runOnUiThread(new ItineraryUpdater());
				}

				if (currentDestinationIndex >= 0) {
					Calendar fiveMinutesBeforeDeparture = Calendar.getInstance();
					fiveMinutesBeforeDeparture.setTime(currentlyAt.getSchedule().getDepartureTime());
					fiveMinutesBeforeDeparture.add(Calendar.MINUTE, -5);
					if (fiveMinutesBeforeDeparture.getTime().before(new Date())) {
						// if ((currentDestinationIndex + 1) < (itineraryItemList.size() - 2)) {
						Itinerary.this.runOnUiThread(new DepartureReminder(currentlyAt, itineraryItemList.get(currentDestinationIndex + 1)));
						// }
					}
				}
			}
		}.start();
	}

	private Address getLocationAddress(Location location) {
		Address theAddress = null;
		try {
			List<Address> addresses = theGeocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
			if (!addresses.isEmpty()) theAddress = addresses.get(0);
		} catch (IOException e) {
			Log.e("Itinerary", e.getMessage(), e);
		}
		return theAddress;
	}

	private void GetMyLocationInfo() {
		if (currentLocation == null) {
			waitForGps();
		} else {
			ItineraryItem myLocation = null;

			// TODO: Wait for location service if current location is null

			try {
				ItineraryItem lastDestination = getFinalDestination();
				myLocation = new ItineraryItem(currentLocation, lastDestination.getLocation(), getLocationAddress(currentLocation));
			} catch (IllegalStateException e) {
				myLocation = new ItineraryItem(currentLocation, getLocationAddress(currentLocation));
			}

			updateArrivalDepartureTimes(myLocation);
		}
	}

	private int getFinalDestinationIndex() {
		if (itineraryItemList.size() < 2) {
			throw new IllegalStateException("The destination list has not been initialize correctly.");
		}

		return (itineraryItemList.size() - 2); // was -1
	}

	private ItineraryItem getFinalDestination() {
		return itineraryItemList.get(getFinalDestinationIndex());
	}

	private int getAppendDestinationIndex() {
		if (itineraryItemList.size() < 2) {
			throw new IllegalStateException("The destination list has not been initialize correctly.");
		}

		return (itineraryItemList.size() - 1);
	}

	private void appendDestination(ItineraryItem newItem) {
		insertDestination(getAppendDestinationIndex(), newItem);
	}

	private void removeDestination(int index) {
		itineraryItems.remove(itineraryItemList.get(index));
		itineraryItemList.remove(index);
	}

	private void insertDestination(int index, ItineraryItem destination) {
		itineraryItemList.add(index, destination);
		itineraryItems.insert(destination, index);
	}

	private void replaceDestination(int index, ItineraryItem destination) {
		removeDestination(index);
		insertDestination(index, destination);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case GET_NEW_DESTINATION:
			if (resultCode == Activity.RESULT_OK) {
				ItineraryItem newDestination = (ItineraryItem) data.getParcelableExtra("itineraryItem");
				updateArrivalDepartureTimes(newDestination);
			}
			break;
		case ADD_DESTINATION:
			if (resultCode == Activity.RESULT_OK) {
				ItineraryItem scheduledDestination = (ItineraryItem) data.getParcelableExtra("destination");
				appendDestination(scheduledDestination);
			}
			break;
		case UPDATE_DESTINATION:
			if (resultCode == Activity.RESULT_OK) {
				ItineraryItem updatedDestination = (ItineraryItem) data.getParcelableExtra("destination");

				if (selectedItemPosition != -1) {
					replaceDestination(selectedItemPosition, updatedDestination);
				}

				if (selectedItemPosition == 0) {
					origin = updatedDestination;
				}

				selectedItemPosition = -1;

				Itinerary.this.runOnUiThread(new ItineraryUpdater());
			}
			break;
		default:
		}
	}

	private void updateArrivalDepartureTimes(ItineraryItem newDestination) {
		Intent startDestinationSchedule = new Intent(Itinerary.this, DestinationScheduleActivity.class);
		Calendar arrivalTimeCalculator = Calendar.getInstance();
		if (!itineraryItemList.contains(newDestination)) {
			if (!itineraryItemList.isEmpty()) {
				ItineraryItem lastDestination = getFinalDestination();
				arrivalTimeCalculator.setTime(lastDestination.getSchedule().getDepartureTime());
				arrivalTimeCalculator.add(Calendar.SECOND, newDestination.getTravelDuration().intValue());
			} else {
				arrivalTimeCalculator.add(Calendar.SECOND, newDestination.getTravelDuration().intValue());
			}
			newDestination.getSchedule().setArrivalTime(arrivalTimeCalculator.getTime());
			startDestinationSchedule.putExtra("vanleer.android.aeon.destination", newDestination);
			startActivityForResult(startDestinationSchedule, ADD_DESTINATION);
		} else {
			startDestinationSchedule.putExtra("vanleer.android.aeon.destination", newDestination);
			startActivityForResult(startDestinationSchedule, UPDATE_DESTINATION);
		}
	}

	public void onClick(View v) {
		// TODO Auto-generated method stub

	}
}
