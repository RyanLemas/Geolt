package ca.brocku.geoit;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import ca.brocku.geoit.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // This will delay the listview so it has time to generate.
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                getActivity();
                ((MainActivity) requireActivity()).listMarkers();
            }
        }, 0);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}