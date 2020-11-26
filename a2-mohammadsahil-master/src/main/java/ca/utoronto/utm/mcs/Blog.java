package ca.utoronto.utm.mcs;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.*;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;

public class Blog implements HttpHandler {
	
	private MongoClient db;
	
	@Inject
	public Blog(MongoClient db) {
		this.db = db;
	}
	
	@Override
	public void handle(HttpExchange r) throws IOException {
		try {
			if (r.getRequestMethod().equals("DELETE"))
				handleDelete(r);
			else if (r.getRequestMethod().equals("GET"))
				handleGet(r);
			else if (r.getRequestMethod().equals("PUT"))
				handlePut(r);
			else
				r.sendResponseHeaders(405, -1);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void handleDelete(HttpExchange r) throws IOException, JSONException {
		try {
			String body = Utils.convert(r.getRequestBody());
			try {
				JSONObject check = new JSONObject(body);
			} catch(JSONException e) {
				r.sendResponseHeaders(400, -1);
				return;
			}
			
			JSONObject deserialized = new JSONObject(body);
			String id = "";
			
			if(deserialized.has("_id")) {
				id = deserialized.getString("_id");
					MongoCollection<Document> posts = db.getDatabase("csc301a2").getCollection("posts");
					
					if(!ObjectId.isValid(id)) {
						r.sendResponseHeaders(400, -1);
						return;
					}
					
					FindIterable<Document> document = posts.find(eq("_id", new ObjectId(id)));
					Iterator<Document> iter = document.iterator();
					
					if(!iter.hasNext()) {
						r.sendResponseHeaders(404, -1);
						return;
					}

					Bson filter = eq("_id", new ObjectId(id));
					DeleteResult result = posts.deleteOne(filter);	
					r.sendResponseHeaders(200, -1);
					return;
			} else {
				r.sendResponseHeaders(400, -1);
				return;
			}
			
		} catch(Exception e) {
			r.sendResponseHeaders(500, -1);
			return;
		}
	}
	
	public void handleGet(HttpExchange r) throws IOException, JSONException {
		try {
			String body = Utils.convert(r.getRequestBody());
			try {
				JSONObject check = new JSONObject(body);
			} catch(JSONException e) {
				r.sendResponseHeaders(400, -1);
				return;
			}

			JSONObject deserialized = new JSONObject(body);
			String id = "";
			String title = "";

			if(deserialized.has("_id")) {
				id = deserialized.getString("_id");
				
					MongoCollection<Document> posts = db.getDatabase("csc301a2").getCollection("posts");
					if(!ObjectId.isValid(id)) {
						r.sendResponseHeaders(400, -1);
						return;
					}
					FindIterable<Document> document = posts.find(eq("_id", new ObjectId(id)));

					Iterator<Document> iter = document.iterator();
					List<String> docs = new ArrayList<>();
					while(iter.hasNext()) {
						docs.add((iter.next()).toJson().toString());
					}
					if(docs.size()==0) {
						r.sendResponseHeaders(404, -1);
						return;
					}

					String response = docs.toString();			
					r.sendResponseHeaders(200, response.length());
					OutputStream os = r.getResponseBody();
					os.write(response.getBytes());
					os.close();
					return;

			} else if(deserialized.has("title")) {
				title = deserialized.getString("title");
		
					MongoCollection<Document> posts = db.getDatabase("csc301a2").getCollection("posts");
					FindIterable<Document> document = posts.find().sort(new BasicDBObject("title", 1));
					Iterator<Document> iter = document.iterator();
					List<JSONObject> docs = new ArrayList<>();
					
					while(iter.hasNext()) {
						Document doc = iter.next();
						JSONObject checker = new JSONObject(doc.toJson());
						if (checker.getString("title").contains(title)) {
							docs.add(checker);
						}
					}
					if(docs.size()==0) {
						r.sendResponseHeaders(404, -1);
						return;
					}
					String response = docs.toString();			
					r.sendResponseHeaders(200, response.length());
					OutputStream os = r.getResponseBody();
					os.write(response.getBytes());
					os.close();
					return;
			} else {				
				r.sendResponseHeaders(400, -1);		
				return;
			}
			
		} catch(Exception e) {
			r.sendResponseHeaders(500, -1);
			return;
		}
	}
	
	public void handlePut(HttpExchange r) throws IOException, JSONException {
		JSONObject obj = new JSONObject();
		String response = "";
		String title = "";
		String author = "";
		String content = "";
		List<String> tags = new ArrayList<>();
		
		try {
			String body = Utils.convert(r.getRequestBody());
			try {
				JSONObject check = new JSONObject(body);
			} catch(JSONException e) {
				r.sendResponseHeaders(400, -1);
				return;
			}
			
			JSONObject deserialized = new JSONObject(body);
			JSONArray jsonArray = new JSONArray();
			
			if(deserialized.has("title") && deserialized.has("author") && deserialized.has("content") 
					&& deserialized.has("tags")) {
				try {
					jsonArray = deserialized.getJSONArray("tags");
				} catch (Exception e) {
					r.sendResponseHeaders(400, -1);
					return;
				}
				title = deserialized.getString("title");
				author = deserialized.getString("author");
				content = deserialized.getString("content");
				for(int i = 0; i < jsonArray.length(); i++){
					tags.add(jsonArray.getString(i));
				}			
			} else {
				r.sendResponseHeaders(400, -1);
				return;
			}

				MongoCollection<Document> posts = db.getDatabase("csc301a2").getCollection("posts");
				Document post = new Document();
				post.append("title", title);
				post.append("author", author);
				post.append("content", content);
				post.append("tags", tags);
				posts.insertOne(post);
				
				ObjectId id = post.getObjectId("_id");
				obj.put("_id", id);
				response = obj.toString();
				r.sendResponseHeaders(200, response.length());
				
				OutputStream os = r.getResponseBody();
				os.write(response.getBytes());
				os.close();
				return;
				
			} catch(Exception e) {
				r.sendResponseHeaders(500, -1);
				e.printStackTrace();
			}
	}

}
