package de.fmaul.android.cmis.repo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;

import de.fmaul.android.cmis.utils.FeedUtils;

public class CmisItem {

	private String title;
	private String downLink;
	private String author;
	private String contentUrl;
	private String id;
	private String mimeType;
	private Date modificationDate;
	
	private Map<String, CmisProperty> properties;

	private CmisItem() {
	}

	public Map<String, CmisProperty> getProperties() {
		return properties;
	}
	
	public String getTitle() {
		return title;
	}

	@Override
	public String toString() {
		return getTitle();
	}

	public String getAuthor() {
		return author;
	}

	public boolean hasChildren() {
		return downLink != null && downLink.length() > 0;
	}

	public String getDownLink() {
		return downLink;
	}
	
	public String getContentUrl() {
		return contentUrl;
	}

	public String getId() {
		return id;
	}

	public String getMimeType() {
		return mimeType;
	}

	public Date getModificationDate() {
		return modificationDate;
	}

	public static CmisItem createFromFeed(Element entry) {
		CmisItem cmi = new CmisItem();
		cmi.parseEntry(entry);
		return cmi;
	}

	private void parseEntry(Element entry) {
		title = entry.element("title").getText();
		id = entry.element("id").getText();
		downLink = "";
		contentUrl = "";
		mimeType = "";
		author = entry.element("author").element("name").getText();
		modificationDate = parseXmlDate(entry.element("updated").getText());

		Element contentElement = entry.element("content");
		if (contentElement != null) {
			contentUrl = contentElement.attributeValue("src");
			mimeType = contentElement.attributeValue("type");
		}

		for (Element link : (List<Element>) entry.elements("link")) {
			if ("down".equals(link.attribute("rel").getText())) {

				if (link.attribute("type").getText().startsWith(
						"application/atom+xml")) {
					downLink = link.attribute("href").getText();
				}
			}
		}
		
		properties = FeedUtils.getCmisPropertiesForEntry(entry);
	}

	private Date parseXmlDate(String date) {
		// 2009-11-03T11:55:39.495Z
		Date modfiedDate = null;
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
		try {
			modfiedDate = df.parse(date);
		} catch (ParseException e) {
			// meh
		}
		return modfiedDate;
	}

	public static CmisItem create(String title, String upLink) {
		 CmisItem cmisItem = new CmisItem();
		 cmisItem.title = title;
		 cmisItem.downLink = upLink;
		 cmisItem.id = "";
		 cmisItem.author = "";
		 cmisItem.contentUrl = null;
		 cmisItem.properties = new HashMap<String, CmisProperty>();
		return cmisItem;
	}
	
	

}