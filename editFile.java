import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class Test {
	private static final String dir = "E:\\vstsworkspace\\project2009\\source\\server\\extension\\src\\";
	private static final HashMap<String, String> file_package = new HashMap<>();
	static{
		try {
			try(BufferedReader br = new BufferedReader(new FileReader("files.txt"))){
				String line;
				while ((line = br.readLine()) != null) {
					int lastdot = line.lastIndexOf(".");
					String packageName = line.substring(0, lastdot);
					String className = line.substring(lastdot + 1, line.length());
					file_package.put(className + ".java", packageName);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
static int anInt = 1;
	public static void main(String[] args) {
		System.out.println(file_package);
		File f = new File(dir);
		updateFile(f);
	}

	static void updateFile(File f) {
		if (f.isDirectory()) {
			for (File sf : f.listFiles()) {
				updateFile(sf);
			}
		} else {
//			System.out.println(f.getName());
			if (file_package.containsKey(f.getName())) {
				try (BufferedReader br = new BufferedReader(new FileReader(f))) {
					String line;
					StringBuilder stringBuilder = new StringBuilder();
					boolean imported = false;
					while ((line = br.readLine()) != null) {
						int index = line.indexOf("package");
						if (index >= 0) {
							String packagename = line.substring(index + 7, line.indexOf(";")).trim();
							if(!file_package.get(f.getName()).equals(packagename)){
//								System.out.println(file_package.get(f.getName()));
								return;
							}
							System.out.println(String.valueOf(anInt++) + ". " + f.getName() + "=" + packagename);
						}
						if (line.startsWith("import") && !imported) {
							stringBuilder.append("import com.altratek.altraserver.extensions.annotation.BeginDate;\r\n");
							imported = true;
						}
						index = line.indexOf("extends HolidayResponder");
						if (index >= 0) {
							stringBuilder.append("@BeginDate(\"2000-01-01\")\r\n");
						}
						stringBuilder.append(line).append("\r\n");
					}
					RandomAccessFile randomAccessFile = new RandomAccessFile(f, "rw");
					randomAccessFile.write(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
					randomAccessFile.close();
				} catch (Throwable t) {
					t.printStackTrace();
					System.exit(0);
					return;
				}
			}
		}
	}
}
