package vanleer.android.aeon;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

class ItineraryItemAdapter extends ArrayAdapter<ItineraryItem> {

	private ArrayList<ItineraryItem> destinationList;

	public ItineraryItemAdapter(Context context, int textViewResourceId, ArrayList<ItineraryItem> items) {
		super(context, textViewResourceId);
		destinationList = items;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi =
					(LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.itinerary_item, null);
		}
		if(!destinationList.isEmpty()) {
			ItineraryItem item = destinationList.get(position);
			if (item != null) {
				TextView destinationName = (TextView) v.findViewById(R.id.textView_destinationName);
				TextView stayDuration = (TextView) v.findViewById(R.id.textView_stayDuration);
				TextView arrivalVicinity = (TextView) v.findViewById(R.id.textView_arrivalLocation);
				TextView arrivalTime = (TextView) v.findViewById(R.id.textView_arrivalTime);
				TextView departureVicinity = (TextView) v.findViewById(R.id.textView_departureLocation);
				TextView departureTime = (TextView) v.findViewById(R.id.textView_departureTime);
				TextView travelDistance = (TextView) v.findViewById(R.id.textView_travelDistance);
				TextView travelTime = (TextView) v.findViewById(R.id.textView_travelTime);

				//TODO: Get type of transportation used
				travelDistance.setText("Drive/Walk/Bike/Ride " + item.GetFormattedDistance());
				travelTime.setText(" in " + item.GetTravelDurationLongFormat());
				arrivalVicinity.setText("Arrive at " + item.GetVicinity());
				arrivalTime.setText(" at " + item.GetArrivalTimeString());
				destinationName.setText(item.GetName());
				stayDuration.setText(" for " + item.GetStayDurationLongFormat());
				departureVicinity.setText("Depart from " + item.GetVicinity());
				departureTime.setText(" at " + item.GetDepartureTimeString());
				
				if(position == 0) {
					arrivalVicinity.setVisibility(View.GONE);
					arrivalTime.setVisibility(View.GONE);
					travelDistance.setVisibility(View.GONE);
					travelTime.setVisibility(View.GONE);
					stayDuration.setVisibility(View.GONE);
					departureVicinity.setVisibility(View.VISIBLE);
					departureTime.setVisibility(View.VISIBLE);

					departureVicinity.setText("Start from " + item.GetVicinity());
				} else if(position == (destinationList.size() - 1)) {
					travelDistance.setVisibility(View.VISIBLE);
					travelTime.setVisibility(View.VISIBLE);
					arrivalVicinity.setVisibility(View.VISIBLE);
					arrivalTime.setVisibility(View.VISIBLE);
					stayDuration.setVisibility(View.GONE);
					departureVicinity.setVisibility(View.GONE);
					departureTime.setVisibility(View.GONE);
					
					arrivalVicinity.setText("End at " + item.GetVicinity());
				} else {
					travelDistance.setVisibility(View.VISIBLE);
					travelTime.setVisibility(View.VISIBLE);
					arrivalVicinity.setVisibility(View.VISIBLE);
					arrivalTime.setVisibility(View.VISIBLE);
					stayDuration.setVisibility(View.VISIBLE);
					departureVicinity.setVisibility(View.VISIBLE);
					departureTime.setVisibility(View.VISIBLE);
				}

			}
		}
		return v;
	}
}