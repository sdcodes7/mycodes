# mycodes

This code is for downloading pdf file from server with progressdialog and also you can view pdf

    RecyclerView recyclerView;
    ProgressDialog myDialog;
    AsmAdapter asmAdapter;
    Context context;
    String get_clsss_di, get_section_id, fileName;
    Prefs prefs;
    private int STORAGE_PERMISSION_CODE = 23;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignments);
        recyclerView = findViewById(R.id.recyclerView);
        setTitle("Assignments");
        context = Assignments.this;
        prefs = new Prefs(context);
        myDialog = Utils.showProgressDialog(context, "Please Wait..");
        Prefs prefs = new Prefs(context);
        get_clsss_di = prefs.getClassID();
        get_section_id = prefs.getSectionID();
        String extStorageDirectory = Environment.getExternalStorageDirectory()
                .toString();
        File folder = new File(extStorageDirectory, "/IpsSchool");
        folder.mkdir();
        show_work(get_clsss_di, get_section_id);
        requestStoragePermission();
    }

    public class AsmAdapter extends RecyclerView.Adapter<AsmAdapter.Holder> {

        List<DiaryModel> dataList;
        private Context context;

        AsmAdapter(Context context, List<DiaryModel> dataList) {
            this.context = context;
            this.dataList = dataList;
        }

        class Holder extends RecyclerView.ViewHolder {

            final View mView;
            public TextView txtdate, txttitle;
            CardView cardView;

            Holder(View itemView) {
                super(itemView);
                mView = itemView;
                txttitle = itemView.findViewById(R.id.txttitle);
                txtdate = itemView.findViewById(R.id.txtdate);
                cardView = itemView.findViewById(R.id.card_view);


            }
        }

        @Override
        public AsmAdapter.Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View view = layoutInflater.inflate(R.layout.viewasm, parent, false);
            return new AsmAdapter.Holder(view);
        }

        @Override
        public void onBindViewHolder(AsmAdapter.Holder holder, final int position) {
            String status = dataList.get(position).getStatus();
            if (status.matches("1")) {
                holder.txttitle.setText(dataList.get(position).getAssignmentName());
                holder.txtdate.setText(dataList.get(position).getAssignmentDate());
                holder.cardView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String urls = dataList.get(position).getFileiD();
                        String filetype = dataList.get(position).getFileEXT();
                        if (filetype.matches("pdf")) {

                            new DownloadFile().execute(urls);

                        } else {
                            String idss = dataList.get(position).getFileiD();
                            prefs.insert_gallery(idss);
                            startActivity(new Intent(context, GallerySingleImage.class));
                        }

                    }
                });


            } else {

                recyclerView.setVisibility(View.GONE);
                Utils.alertdialog(context, "No Home Work Found");
            }


        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

    }

    public void show_work(String classid, String sectionid) {
        myDialog.show();
        GetDataService service = RetrofitClientInstance.getRetrofitInstance().create(GetDataService.class);

        Call<List<DiaryModel>> call = service.getasm(classid, sectionid);
        call.enqueue(new Callback<List<DiaryModel>>() {
            @Override
            public void onResponse(Call<List<DiaryModel>> call, Response<List<DiaryModel>> response) {
                myDialog.dismiss();

                List<DiaryModel> obj = response.body();
                String status = obj.get(0).getStatus();
                if (status.matches("0")) {
                    Utils.alertdialog(context, "No Assignment Found");
                    recyclerView.setAdapter(null);
                } else {
                    generateDataList(response.body());

                }

            }

            @Override
            public void onFailure(Call<List<DiaryModel>> call, Throwable t) {
                myDialog.dismiss();
                Toast.makeText(context, "Something went wrong...Please try later!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*Method to generate List of data using RecyclerView with custom adapter*/
    private void generateDataList(List<DiaryModel> dashboardModelList) {
        recyclerView.setVisibility(View.VISIBLE);

        asmAdapter = new AsmAdapter(this, dashboardModelList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(asmAdapter);
    }

    private class DownloadFile extends AsyncTask<String, String, String> {

        private ProgressDialog progressDialog;

        private String folder;
        private boolean isDownloaded;

        /**
         * Before starting background thread
         * Show Progress Bar Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.progressDialog = new ProgressDialog(context);
            this.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            this.progressDialog.setCancelable(false);
            this.progressDialog.show();
        }

        /**
         * Downloading file in background thread
         */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection connection = url.openConnection();
                connection.connect();
                // getting file length
                int lengthOfFile = connection.getContentLength();


                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);
                //Extract file name from URL
                fileName = f_url[0].substring(f_url[0].lastIndexOf('/') + 1, f_url[0].length());

                //  Append timestamp to file name
                // fileName = timestamp + "_" + fileName;

                //External directory path to save file
                folder = Environment.getExternalStorageDirectory() + File.separator + "IpsSchool/";

                //Create androiddeft folder if it does not exist
                File directory = new File(folder);

                if (!directory.exists()) {
                    directory.mkdirs();
                }

                // Output stream to write file
                OutputStream output = new FileOutputStream(folder + fileName);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lengthOfFile));
                    //  Log.d(TAG, "Progress: " + (int) ((total * 100) / lengthOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();
                return "Downloaded at: " + folder + fileName;

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return "Something went wrong";
        }

        /**
         * Updating progress bar
         */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            progressDialog.setProgress(Integer.parseInt(progress[0]));
        }


        @Override
        protected void onPostExecute(String message) {
            // dismiss the dialog after the file was downloaded
            if (message != null) {
                // Do you work here on success
            } else {
                // null response or Exception occur
            }
            this.progressDialog.dismiss();

            // Display File path after downloading
             Toast.makeText(getApplicationContext(),message, Toast.LENGTH_LONG).show();
            openpdf();


        }
    }


    public void openpdf() {
        File file = null;
        file = new File(Environment.getExternalStorageDirectory() + "/IpsSchool/" + fileName);
        //    Toast.makeText(getApplicationContext(), file.toString(), Toast.LENGTH_LONG).show();
        //if (file.exists()) {
        Intent target = new Intent(Intent.ACTION_VIEW);
        target.setDataAndType(Uri.fromFile(file), "application/pdf");
        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        try {
            Intent intent = Intent.createChooser(target, "Open File");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Instruct the user to install a PDF reader here, or something
        }}}}
      

    private void requestStoragePermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

        }

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
    }

    //This method will be called when the user will tap on allow or deny
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        //Checking the request code of our request
        if (requestCode == STORAGE_PERMISSION_CODE) {

            //If permission is granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                //Displaying a toast
                //     Toast.makeText(this,"Permission granted now you can read the storage",Toast.LENGTH_LONG).show();
            } else {
                //Displaying another toast if permission is not granted
                //   Toast.makeText(this,"Oops you just denied the permission",Toast.LENGTH_LONG).show();
            }
        }
    }
 
 
