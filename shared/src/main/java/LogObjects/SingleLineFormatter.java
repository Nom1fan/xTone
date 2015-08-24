package LogObjects;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class SingleLineFormatter extends Formatter {

	public String format(LogRecord record) {
		
		String newline = System.getProperty("line.separator");
		String spaces = "";
		String level = record.getLevel().toString();
		
		if(level.equals("SEVERE"))
			spaces = " ";
		else if(level.equals("INFO"))
			spaces = "   ";
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy [HH:mm:ss]");
		String date = sdf.format(new Date());
						
	    return date + record.getLevel() + spaces +"::"
	        + record.getSourceClassName() + "::"
	        + record.getSourceMethodName()+ "()" + "::"
	        + record.getMessage() + newline;
	}

}