package newtobacco;

@SuppressWarnings("serial")
public class NewTobaccoException extends Exception {

	public NewTobaccoException(String responseString) {
		super(responseString);
	}

	public NewTobaccoException(Exception e) {
		super(e);
	}

	public NewTobaccoException(String string, Throwable e) {
		super(string, e);
	}
}