package spl;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.sciplore.beans.Author;
import org.sciplore.beans.Document;
import org.sciplore.beans.Year;
import org.sciplore.formatter.Bean;
import org.sciplore.formatter.SimpleTypeElementBean;

/**
 * Created by IntelliJ IDEA.
 * User: Christoph Arbeit
 * Date: 09.09.2010
 * Time: 10:56:50
 * To change this template use File | Settings | File Templates.
 */
public class DocumentsWrapper {

    Document xmlDocuments;

    public DocumentsWrapper(Document documents) {
        this.xmlDocuments = documents;
    }

    public Document getXmlDocuments() {
        return xmlDocuments;
    }

    public void setXmlDocuments(Document xmlDocuments) {
        this.xmlDocuments = xmlDocuments;
    }

    public List<Vector> getDocuments(){
        List<Vector> documents = new ArrayList<Vector>();
        //List<XmlDocument> xmlDocuments = this.xmlDocuments.getDocuments();
        //for(XmlDocument xmlDocument : xmlDocuments){
        	Document xmlDocument = xmlDocuments;
            Vector<String> vector = new Vector<String>();
            if(xmlDocument.getTitle() != null){
                vector.add(xmlDocument.getTitle().getValue());
            }
            else{
                vector.add("");
            }
            if(xmlDocument.getAuthors() != null){
                List<Bean> authors = xmlDocument.getAuthors().getCollection();
                String value = "";
                int i = 1;
                for(Bean author : authors){
                    if(i < authors.size()){
                        value = value + getNameComplete(((Author)author));
                        value = value + ", ";
                    }
                    else{
                        value = value + getNameComplete(((Author)author));
                    }
                    i++;
                }
                vector.add(value);
            }
            else{
                vector.add("");
            }
            if(xmlDocument.getYear() != null && ((Year)xmlDocument.getYear()).getValue() != null && !((Year)xmlDocument.getYear()).getValue().equalsIgnoreCase("null")){
                vector.add(((Year)xmlDocument.getYear()).getValue());
            }
            /*if(xmlDocument.getPublishdate() != null && xmlDocument.getPublishdate().getYear() != null && !xmlDocument.getPublishdate().getYear().equalsIgnoreCase("null")){
                vector.add(xmlDocument.getPublishdate().getYear());
            }*/
            else{
                vector.add("");
            }
            documents.add(vector);
        //}
        return documents;
    }
    
    private String getNameComplete(Author author){
    	if(author == null) return "";
    	String result = "";
    	if(getSimpleTypeValue(author.getName_First()) != null)
    		result = result + getSimpleTypeValue(author.getName_First()).trim() + " ";
    	if(getSimpleTypeValue(author.getName_Middle()) != null)
    		result = result + getSimpleTypeValue(author.getName_Middle()).trim() + " ";
    	if(getSimpleTypeValue(author.getName_Last_Prefix()) != null)
    		result = result + getSimpleTypeValue(author.getName_Last_Prefix()).trim() + " ";
    	if(getSimpleTypeValue(author.getName_Last()) != null)
    		result = result + getSimpleTypeValue(author.getName_Last()).trim() + " ";
    	if(getSimpleTypeValue(author.getName_Last_Suffix()) != null)
    		result = result + getSimpleTypeValue(author.getName_Last_Suffix()).trim() + " ";
    	return result.trim();
    }
    
    private String getSimpleTypeValue(Bean bean){
    	if(bean == null || !(bean instanceof SimpleTypeElementBean)) return null;
    	SimpleTypeElementBean simpleTypeElementBean = (SimpleTypeElementBean)bean;
    	if(simpleTypeElementBean.getValue() == null || simpleTypeElementBean.getValue().equalsIgnoreCase("null") || simpleTypeElementBean.getValue().length() <= 0) return null;
    	return simpleTypeElementBean.getValue();
    }
    
    private List<Bean> sortAuthors(List<Bean> authors){
    	for(Bean author : authors){
    		int rank = Integer.parseInt(((Author)author).getRank());
    		//for()
    	}
    	return authors;
    }
}
