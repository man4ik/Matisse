/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.sample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.support.media.ExifInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class SampleActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE_CHOOSE = 23;

    private UriAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.zhihu).setOnClickListener(this);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter = new UriAdapter());
    }

    @Override
    public void onClick(final View v) {
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        if (aBoolean) {
                            switch (v.getId()) {
                                case R.id.zhihu:
                                    Matisse.from(SampleActivity.this)
                                            .choose(MimeType.ofImage())
                                            .countable(true)
                                            .maxSelectable(1)
                                            .showSingleMediaType(true)
                                            .gridExpectedSize(getResources().getDimensionPixelSize(R.dimen.grid_expected_size))
                                            .restrictOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                                            .thumbnailScale(0.85f)
                                            .showSingleMediaType(true)
                                            .imageEngine(new GlideEngine())
                                            .forResult(REQUEST_CODE_CHOOSE);
                                    break;
                            }
                            mAdapter.setData(null, null, null);
                        } else {
                            Toast.makeText(SampleActivity.this, R.string.permission_request_denied, Toast.LENGTH_LONG)
                                    .show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK) {
            List<Uri> uris = Matisse.obtainResult(data);
            try {
                InputStream inputStream = getContentResolver().openInputStream(uris.get(0));
                ExifInterface exifInterface = new ExifInterface(inputStream);
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> fromLocation = geocoder.getFromLocation(exifInterface.getLatLong()[0], exifInterface.getLatLong()[1], 10);
                mAdapter.setData(uris, Matisse.obtainPathResult(data), fromLocation);

            } catch (Exception e) {
                new AlertDialog.Builder(this).setMessage("1:" + e.getMessage()).show();
            }

/*
            var inputStream: InputStream? = null
            try {
                inputStream = view?.context()?.contentResolver?.openInputStream(imageUri)
                val exifInterface = ExifInterface(inputStream)
                val geocoder = Geocoder(view?.context(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(exifInterface.latLong[0], exifInterface.latLong[1], 1)
                return addresses[0]
            } catch (e: Exception) {
            } finally {
                try {
                    inputStream?.close()
                } catch (ignored: IOException) {
                }
            }*/
        }
    }

    private static class UriAdapter extends RecyclerView.Adapter<UriAdapter.UriViewHolder> {

        private List<Uri> mUris;
        private List<String> mPaths;
        private List<Address> mAddresses;

        void setData(List<Uri> uris, List<String> paths, List<Address> addresses) {
            mUris = uris;
            mPaths = paths;
            mAddresses = addresses;
            notifyDataSetChanged();
        }

        @Override
        public UriViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new UriViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.uri_item, parent, false));
        }

        @Override
        public void onBindViewHolder(UriViewHolder holder, int position) {
            try {
                holder.mUri.setText(mUris.get(position).toString());
                holder.mPath.setText(mPaths.get(position));
                Address address = mAddresses.get(position);
                holder.mAddress.setText(address.getCountryName() + ", " + address.getLocality());

                holder.mUri.setAlpha(position % 2 == 0 ? 1.0f : 0.54f);
                holder.mPath.setAlpha(position % 2 == 0 ? 1.0f : 0.54f);
            } catch (Exception e) {
                new AlertDialog.Builder(holder.mUri.getContext()).setMessage(e.getMessage()).show();
            }
        }

        @Override
        public int getItemCount() {
            return mUris == null ? 0 : mUris.size();
        }

        static class UriViewHolder extends RecyclerView.ViewHolder {

            private TextView mUri;
            private TextView mPath;
            private TextView mAddress;

            UriViewHolder(View contentView) {
                super(contentView);
                mUri = (TextView) contentView.findViewById(R.id.uri);
                mPath = (TextView) contentView.findViewById(R.id.path);
                mAddress = (TextView) contentView.findViewById(R.id.address);
            }
        }
    }

}
