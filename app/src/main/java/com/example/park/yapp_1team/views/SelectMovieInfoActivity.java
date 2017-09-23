package com.example.park.yapp_1team.views;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.example.park.yapp_1team.R;
import com.example.park.yapp_1team.adapters.SelectMovieRecyclerViewAdapter;
import com.example.park.yapp_1team.interfaces.RcvClickListener;
import com.example.park.yapp_1team.items.CGVRealmModel;
import com.example.park.yapp_1team.items.LotteRealmModel;
import com.example.park.yapp_1team.items.MegaboxRealmModel;
import com.example.park.yapp_1team.items.SelectMovieInfoItem;
import com.example.park.yapp_1team.items.TheaterDisInfo;
import com.example.park.yapp_1team.sql.RealmRest;
import com.example.park.yapp_1team.utils.Strings;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.realm.RealmResults;

import static com.example.park.yapp_1team.utils.PermissionRequestCode.LOCATION_PERMISSION_CODE;

public class SelectMovieInfoActivity extends BaseActivity {

    private static final String TAG = SelectMovieInfoActivity.class.getSimpleName();

    private RecyclerView rcvSelectMovie;
    private FloatingActionButton fabSelectMovieInfoBack;
    private FloatingActionButton fabMovieFilter;
    private Toolbar selectToolbar;

    private SelectMovieRecyclerViewAdapter adapter;
    private int count = 0;

    private List<String> names;

    private RealmRest realmRest;

    private double lat, lng;
    private Location mCurrentLocation;
    private FusedLocationProviderClient mFusedLocationProviderClient;


    private RcvClickListener clickListener = new RcvClickListener() {
        @Override
        public void itemClick(int position) {
            // TODO: 2017-09-08 item click event
            startActivity(new Intent(getApplicationContext(), MapActivity.class));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_movie_info);
        statusBarChange();
        // TODO: 2017-09-02 intent로 count 받아오기
        Intent intent = getIntent();
        names = intent.getStringArrayListExtra("names");

        count = names.size();

        Log.e(TAG, String.valueOf(count));

        initialize();
        addItem();
        event();

        permissionCheck();
    }

    private void event() {
        fabSelectMovieInfoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LocationSetupActivity.class);
                startActivity(intent);
            }
        });

        fabMovieFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

    }

    private void initialize() {
        selectToolbar = (Toolbar) findViewById(R.id.toolbar_select_info);
        setSupportActionBar(selectToolbar);
        selectToolbar.setContentInsetsAbsolute(0, 0);

        rcvSelectMovie = (RecyclerView) findViewById(R.id.rcv_select_movie_info);
        fabSelectMovieInfoBack = (FloatingActionButton) findViewById(R.id.btn_select_movie_info_back);
        fabMovieFilter = (FloatingActionButton) findViewById(R.id.btn_select_movie_info_filter);

        if (count > 1) {
            adapter = new SelectMovieRecyclerViewAdapter(this, true);
        } else {
            adapter = new SelectMovieRecyclerViewAdapter(this, false);
        }

        adapter.setRcvClickListener(clickListener);

        rcvSelectMovie.setHasFixedSize(true);
        rcvSelectMovie.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rcvSelectMovie.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        rcvSelectMovie.setAdapter(adapter);

        realmRest = new RealmRest();

    }

    private void addItem() {
        List<SelectMovieInfoItem> list = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            SelectMovieInfoItem item = new SelectMovieInfoItem();
            item.setTitle(names.get(0));
            item.setStartTime("9:00");
            item.setEndTIme(" - 13:00");
            item.setLocation("CGV 청담·2.20km·프리미엄");
            item.setUseSeat("13");
            item.setLeftSeat("/200");

            list.add(item);
            adapter.setListItems(list);
        }
    }


    @SuppressWarnings("MissingPermission")
    private void getLocation() {
        if (!checkGPS()) {
            Toast.makeText(this, "GPS를 확인하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        OnCompleteListener<Location> onCompleteListener = new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    mCurrentLocation = task.getResult();
                    lat = mCurrentLocation.getLatitude();
                    lng = mCurrentLocation.getLongitude();

                    findTheater(lat, lng);

                } else {
                    try {
                        Log.e(TAG, "CurrentLocation exception");
                        throw task.getException();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        mFusedLocationProviderClient.getLastLocation().addOnCompleteListener(this, onCompleteListener);
    }

    private void findTheater(double lat, double lng) {

        List<TheaterDisInfo> disInfo = new ArrayList<>();

        Location currentLocation = new Location("currentPosition");
        currentLocation.setLatitude(lat);
        currentLocation.setLongitude(lng);

        RealmResults<CGVRealmModel> cgvResults = realmRest.getCGVInfo();
        for (int i = 0; i < cgvResults.size(); i++) {
            Location cgvLocation = new Location("cgv");
            double cgvLat = cgvResults.get(i).getLat();
            double cgvLng = cgvResults.get(i).getLng();
            cgvLocation.setLatitude(cgvLat);
            cgvLocation.setLongitude(cgvLng);
            double dis = currentLocation.distanceTo(cgvLocation);

            TheaterDisInfo theaterDisInfo = new TheaterDisInfo();
            theaterDisInfo.name = cgvResults.get(i).getName();
            theaterDisInfo.lat = cgvLat;
            theaterDisInfo.lng = cgvLng;
            theaterDisInfo.distance = dis;
            theaterDisInfo.code = Strings.THEATER_CODE_CGV;

            disInfo.add(theaterDisInfo);
        }

        RealmResults<MegaboxRealmModel> megaResult = realmRest.getMegaInfo();
        for (int i = 0; i < megaResult.size(); i++) {
            Location megaLocation = new Location("megabox");
            double megaLat = megaResult.get(i).getLat();
            double megaLng = megaResult.get(i).getLng();
            megaLocation.setLatitude(megaLat);
            megaLocation.setLongitude(megaLng);
            double dis = currentLocation.distanceTo(megaLocation);

            TheaterDisInfo theaterDisInfo = new TheaterDisInfo();
            theaterDisInfo.name = megaResult.get(i).getName();
            theaterDisInfo.lat = megaLat;
            theaterDisInfo.lng = megaLng;
            theaterDisInfo.distance = dis;
            theaterDisInfo.code = Strings.THEATER_CODE_MEGA;

            disInfo.add(theaterDisInfo);
        }

        RealmResults<LotteRealmModel> lotteResult = realmRest.getLotteInfo();
        for (int i = 0; i < lotteResult.size(); i++) {
            Location lotteLocation = new Location("Lotte");
            double lotteLat = lotteResult.get(i).getLat();
            double lotteLng = lotteResult.get(i).getLng();
            lotteLocation.setLatitude(lotteLat);
            lotteLocation.setLongitude(lotteLng);
            double dis = currentLocation.distanceTo(lotteLocation);

            TheaterDisInfo theaterDisInfo = new TheaterDisInfo();
            theaterDisInfo.name = lotteResult.get(i).getName();
            theaterDisInfo.lat = lotteLat;
            theaterDisInfo.lng = lotteLng;
            theaterDisInfo.distance = dis;
            theaterDisInfo.code = Strings.THEATER_CODE_LOTTE;

            disInfo.add(theaterDisInfo);
        }

        Comparator<TheaterDisInfo> comparator = new Comparator<TheaterDisInfo>() {
            @Override
            public int compare(TheaterDisInfo o1, TheaterDisInfo o2) {
                if (o1.distance > o2.distance) {
                    return 1;
                } else if (o1.distance < o2.distance) {
                    return -1;
                } else {
                    return 0;
                }
            }
        };

        Collections.sort(disInfo, comparator);

        // 가까운 영화관 3개로부터 정보 get
        for (int i = 0; i < 10; i++) {

            String brand = "";
            switch (disInfo.get(i).code) {
                case 1: {
                    brand = "CGV";
                    break;
                }
                case 2: {
                    brand = "MEGA BOX";
                    break;
                }
                case 3: {
                    brand = "Lotte Cinema";
                    break;
                }
            }

            String tag = brand + " " + disInfo.get(i).name + " : " + disInfo.get(i).distance;
            Log.e(TAG, tag);
        }
    }

    private void getMovieInfo(CGVRealmModel realmModel) {
        // TODO: 2017-09-22 크롤링을 통해 영화 이름 비교 + 시간과 정보 가져오기
        // TODO: 2017-09-22 CGV 주소 : http://www.cgv.co.kr/common/showtimes/iframeTheater.aspx?areacode=01&theatercode=0060&date=20170922
        // TODO: 2017-09-23 CGV 주소2 : http://www.cgv.co.kr/theaters/special/show-times.aspx?regioncode=103&theatercode=0040 
        // TODO: 2017-09-23 MEGA 주소 : http://www.megabox.co.kr/?menuId=theater-detail&region=10&cinema=1372#menu3
        // TODO: 2017-09-23 lotte 주소 :  http://www.lottecinema.co.kr/LCHS/Contents/Cinema/Cinema-Detail.aspx?divisionCode=1&detailDivisionCode=1&cinemaID=1013
        // areacode와 theatercode, date를 변경해가며 검색 (date는 기본 오늘)
        // TODO: 2017-09-22 가져온 값을 하나의 리스트에 저장하고 시간 순으로 정렬
    }

    private boolean checkGPS() {
        LocationManager manager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("GPS 상태 변경");
            builder.setMessage("GPS 상태 변경 하러 갈래요?");
            builder.setPositiveButton("Yes",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent it = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            it.addCategory(Intent.CATEGORY_DEFAULT);
                            startActivity(it);
                        }
                    });
            builder.setNegativeButton("No",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
            builder.show();
            return false;
        } else {
            return true;
        }
    }

    private void permissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_CODE);
            } else {
                // TODO: 2017-08-11 already has permission granted, go to next step.
                getLocation();
            }
        } else {
            getLocation();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_select_movie_info, menu);
        return true;
    }
}
