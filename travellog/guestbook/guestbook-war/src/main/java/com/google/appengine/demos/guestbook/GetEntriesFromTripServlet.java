package com.google.appengine.demos.guestbook;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.Transform;
import com.google.appengine.demos.guestbook.JSON.JSONArray;
import com.google.appengine.demos.guestbook.JSON.JSONException;
import com.google.appengine.demos.guestbook.JSON.JSONObject;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.ArrayList;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import java.util.List;

/**
 * 
 parameters: user key, limit (number of trips to get)
 */
public class GetEntriesFromTripServlet extends HttpServlet {

	private BlobstoreService blobstoreService = BlobstoreServiceFactory
			.getBlobstoreService();
	ImagesService imagesService = ImagesServiceFactory.getImagesService();

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {

		DatastoreService datastore = DatastoreServiceFactory
				.getDatastoreService();

		String tripKeyString = req.getParameter("tripKey");
		String limitstring = req.getParameter("limit");
		if (limitstring == null)
			limitstring = "10";
		int limit = Integer.parseInt(limitstring);

		if (tripKeyString == null) {
			resp.sendRedirect("/signin.jsp");
		}
		Query query = new Query("Entry").addFilter("tripPoster",
	               Query.FilterOperator.EQUAL,
	               tripKeyString).addSort("dateCreated", Query.SortDirection.DESCENDING);
	              List<Entity> entries = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(limit));

		if (entries.isEmpty()) {
			System.out.println("no entries");
		}

		//TODO: photos
		// get trips as json file
		JSONObject json = new JSONObject();
		JSONArray entriesjson = new JSONArray();
		JSONObject entryjson;
		JSONArray photos;
		JSONObject photo;
		try {
			for (Entity entry : entries) {
				entryjson = new JSONObject();
				entryjson.put("title", (String) entry.getProperty("title"));
				entryjson.put("description",
						(String) entry.getProperty("description"));
				entryjson.put("key",
						(String) KeyFactory.keyToString(entry.getKey()));
				entryjson.put("location", (String) entry.getProperty("location"));
				entryjson.put("tags", (String) entry.getProperty("tags"));
				entryjson.put("tripKey", tripKeyString);
				entryjson.put("latitude", (String) entry.getProperty("latitude"));
				entryjson.put("longitude", (String) entry.getProperty("longitude"));
				
				//photos
				List<String> entryphotos = (List<String>) entry.getProperty("photos");
				photos = new JSONArray();
				if(entryphotos == null) entryphotos = new ArrayList<String>();
				for(int i = 0; i < entryphotos.size(); i++) {
					Entity photoEntity = datastore.get(KeyFactory.stringToKey(entryphotos.get(i)));
					photo = new JSONObject();
					photo.put("title", photoEntity.getProperty("title"));
					photo.put("description", photoEntity.getProperty("description"));
					photo.put("key", entryphotos.get(i));
					photo.put("blobKey", ((BlobKey) photoEntity.getProperty("blobKey")).getKeyString());
					photos.put(photo);
				}
				entryjson.put("photos", photos);
				entriesjson.put(entryjson);
			}
			json.put("entries", entriesjson);
		} catch (JSONException e) {
			System.out.println("json exception with entries");
			e.printStackTrace();
		} catch (EntityNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("entity photo not found");
			e.printStackTrace();
		}
		resp.setContentType("application/json");
		resp.getWriter().write(json.toString());
		System.out.println(json.toString());
	}
}