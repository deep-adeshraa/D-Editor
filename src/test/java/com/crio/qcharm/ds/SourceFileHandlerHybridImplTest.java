package com.crio.qcharm.ds;

import com.crio.qcharm.log.UncaughtExceptionHandler;
import com.crio.qcharm.request.EditRequest;
import com.crio.qcharm.request.PageRequest;
import com.crio.qcharm.request.SearchReplaceRequest;
import com.crio.qcharm.request.SearchRequest;
import com.crio.qcharm.request.UndoRequest;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

// @ExtendWith(SpringExtension.class)
class SourceFileHandlerHybridImplTest {


  private long getThreadElapsedTime() {
    return ManagementFactory.getThreadMXBean().getCurrentThreadUserTime();
  }

  @BeforeEach
  public void setupUncaughtExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());
  }

  static FileInfo inefficientSearch;
  static List<Cursor> expectedCursorPositions = new ArrayList<>();
  static String pattern;
  @BeforeAll
  static void setup() {

    StringBuffer prefix = new StringBuffer("");

    for (int i = 0; i < 30; ++i) {
      prefix.append("ab");
    }

    pattern = prefix.toString() + "aa";
    patternGenerator(prefix.toString() + "ab", pattern);
  }

  List<String> clone(List<String> lst) {
    return lst.stream().collect(Collectors.toList());
  }

  @AfterAll
  static void teardown() {
    inefficientSearch.getLines().clear();
  }

  static void patternGenerator(String pattern1, String pattern2) {
    StringBuffer buffer1 = new StringBuffer("");
    StringBuffer buffer2 = new StringBuffer("");

    int K = 250;
    for (int i = 0; i < K; ++i) {
      buffer1.append(pattern1);
    }
    buffer1.append(pattern2);

    for (int  i = 0; i < K; ++i) {
      buffer1.append(pattern1);
    }

    List<String> lines = new ArrayList<>();

    int N = 1000;

    String s1 = buffer1.toString();
    for (int i = 0; i < N; ++i) {
      lines.add(s1 + new Integer(i).toString() );
    }

    int len1 = pattern1.length();
    for (int i = 0; i < N; ++i) {
      expectedCursorPositions.add(new Cursor(i, K * len1));
    }

    inefficientSearch =  new FileInfo("testfile", lines);
  }

  FileInfo getLargeSampleFileInfo(String fileName, int n) {
    List<String> testLines = new ArrayList<>();

    for (int i = 0; i < n; ++i) {
      StringBuffer buffer = new StringBuffer("lineno");
      buffer.append(i);
      testLines.add(buffer.toString());
    }
    return new FileInfo(fileName, testLines);
  }

  FileInfo getSampleFileInfo() {
    List<String> testLines = new ArrayList<>();
    testLines.add("def sqr(x):");
    testLines.add(" return x * x");

    return new FileInfo("testfile.txt", testLines);
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void smallFileLoadingReturnsAllLines() {
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler("testfile");

    FileInfo fileInfo = getSampleFileInfo();
    Page page = sourceFileHandlerHybrid.loadFile(fileInfo);

    assertEquals(fileInfo.getLines(), page.getLines());
  }

  private SourceFileHandler getSourceFileHandler(String testfile) {
    return new SourceFileHandlerHybridImpl(testfile);
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void largeFileLoadingReturnsFiftyLinesOfData() {
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler("testfile");

    FileInfo fileInfo = getLargeSampleFileInfo("largeFile", 1000000);
    Page page = sourceFileHandlerHybrid.loadFile(fileInfo);
    assertEquals(fileInfo.getLines().subList(0, 50), page.getLines());
    assertEquals(0, page.getStartingLineNo());
    assertEquals(new Cursor(0,0), page.getCursorAt());
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void getNextLinesReturnsEmptyPageIfThereIsNoLinesAfter() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    FileInfo fileInfo = getLargeSampleFileInfo(fileName, 100);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    int length = 35;
    int startingLineNo = 100;
    Cursor cursor = new Cursor(0, 0);
    PageRequest pageRequest = new PageRequest(startingLineNo, fileName, length, cursor);
    Page emptyPage = sourceFileHandlerHybrid.getNextLines(pageRequest);

    assertEquals(new ArrayList<String>(), emptyPage.getLines());
    assertEquals(startingLineNo, emptyPage.getStartingLineNo());
    assertEquals(cursor, emptyPage.getCursorAt());
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void getNextLinesReturnsLessThanRequestedNumberOfLines() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    FileInfo fileInfo = getLargeSampleFileInfo(fileName, 100);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    int length = 35;
    int startingLine = 90;

    Cursor cursor = new Cursor(0, 0);
    PageRequest pageRequest = new PageRequest(startingLine, fileName, length, cursor);
    Page page = sourceFileHandlerHybrid.getNextLines(pageRequest);

    assertEquals(fileInfo.getLines().subList(startingLine+1, 100), page.getLines());
    assertEquals(startingLine+1, page.getStartingLineNo());
    assertEquals(cursor, page.getCursorAt());
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void getPrevLinesReturnsRequestedNumberOfLines() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    FileInfo fileInfo = getLargeSampleFileInfo(fileName, 100);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    int length = 35;
    int startingLine = 35;

    Cursor cursor = new Cursor(0, 0);
    PageRequest pageRequest = new PageRequest(startingLine, fileName, length, cursor);
    Page page = sourceFileHandlerHybrid.getPrevLines(pageRequest);

    assertEquals(fileInfo.getLines().subList(0, length), page.getLines());
    assertEquals(0, page.getStartingLineNo());
    assertEquals(cursor, page.getCursorAt());
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void getPrevLinesReturnsEmptyPageIfThereIsNoLinesBefore() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    FileInfo fileInfo = getLargeSampleFileInfo(fileName, 100);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    int length = 35;
    Cursor cursor = new Cursor(0, 0);
    PageRequest pageRequest = new PageRequest(0, fileName, length, cursor);
    Page emptyPage = sourceFileHandlerHybrid.getPrevLines(pageRequest);

    assertEquals(new ArrayList<String>(), emptyPage.getLines());
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void getPrevLinesReturnsLessThanRequestedNumberOfLines() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    FileInfo fileInfo = getLargeSampleFileInfo(fileName, 100);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    int length = 35;
    int startingLine = 10;

    Cursor cursor = new Cursor(0, 0);
    PageRequest pageRequest = new PageRequest(startingLine, fileName, length, cursor);
    Page page = sourceFileHandlerHybrid.getPrevLines(pageRequest);

    assertEquals(fileInfo.getLines().subList(0, 10), page.getLines());
    assertEquals(0, page.getStartingLineNo());
    assertEquals(cursor, page.getCursorAt());
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void getNextLinesReturnsRequestedNumberOfLines() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    FileInfo fileInfo = getLargeSampleFileInfo(fileName, 100);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    int length = 35;
    int startingLine = 35;

    Cursor cursor = new Cursor(0, 0);
    PageRequest pageRequest = new PageRequest(startingLine, fileName, length, cursor);
    Page page = sourceFileHandlerHybrid.getNextLines(pageRequest);

    assertEquals(fileInfo.getLines().subList(36, 71), page.getLines());
    assertEquals(36, page.getStartingLineNo());
    assertEquals(cursor, page.getCursorAt());
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void getLinesFromReturnsRequestedNumberOfLines() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    FileInfo fileInfo = getLargeSampleFileInfo(fileName, 100);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    int length = 35;
    int startingLine = 10;

    Cursor cursor = new Cursor(20, 13);
    PageRequest pageRequest = new PageRequest(startingLine, fileName, length, cursor);
    Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    Cursor expectedCursorPosition = new Cursor(startingLine, 0);
    assertEquals(expectedCursorPosition, page.getCursorAt());
    assertEquals(fileInfo.getLines().subList(startingLine, startingLine + length), page.getLines());
    assertEquals(10, page.getStartingLineNo());
  }

  @Test
  @Timeout(value = 20000, unit = TimeUnit.MILLISECONDS)
  void  efficientSearchTest() {
    String fileName = "efficientSearchTest";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    sourceFileHandlerHybrid.loadFile(inefficientSearch);
    SearchRequest searchRequest = new SearchRequest(0, pattern, fileName);
    long timeTakenInNs = 0;
    for (int i = 0; i < 10; ++i) {
      long startTime = getThreadElapsedTime();
      List<Cursor> cursors = sourceFileHandlerHybrid.search(searchRequest);
      timeTakenInNs += getThreadElapsedTime() - startTime;
      assertEquals(expectedCursorPositions, cursors);
    }
    System.out.printf("efficientSearchTest timetaken = %d ns\n", timeTakenInNs);
    assert (timeTakenInNs < 3000000000l);
  }



  @Test
  @Timeout(value = 15000, unit = TimeUnit.MILLISECONDS)
  void randomJumpGivesGoodnPerformance() {
    String fileName = "jump.txt";
    SourceFileHandler sourceFileHandler = getSourceFileHandler(fileName);

    int N = 250000;
    final FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandler.loadFile(fileInfo);

    List<Integer> offsets = new ArrayList<>();

    int incr = N / 10000;
    for (int i = 0; i < 10000; ++i) {
      //offsets.add((int)(Math.random() * N));
      int idx = i * incr;
      offsets.add(idx);
    }

    long timeTakenInNs = 0;
    final int TIMES = 500;
    for (int times = 0; times < TIMES; ++times) {
      Collections.shuffle(offsets);
      for (int i = 0; i < offsets.size(); ++i) {
        int start = offsets.get(i);
        final int numberOfLines = 5;
        final PageRequest pageRequest = new PageRequest(start, fileName, numberOfLines,
            new Cursor(i,
                0));

        long startTime = getThreadElapsedTime();
        final Page page = sourceFileHandler.getLinesFrom(pageRequest);
        timeTakenInNs += getThreadElapsedTime() - startTime;

        int end = Math.min(numberOfLines + start, fileInfo.getLines().size());
        assertEquals(fileInfo.getLines().subList(start, end), page.getLines());
      }
    }
    System.out.println(timeTakenInNs);
    assert(timeTakenInNs < 5000000000l);
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void search() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 100;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    SearchRequest searchRequest = new SearchRequest(0, "lineno", fileName);

    List<Cursor> cursors = sourceFileHandlerHybrid.search(searchRequest);
    List<Cursor> expected = new ArrayList<>();

    for (int i = 0; i < N; ++i) {
      expected.add(new Cursor(i, 0));
    }

    assertEquals(expected, cursors);
  }



  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void editLines() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 100;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    List<String> changedLines = new ArrayList<>();
    for (int i = 0; i < 35; ++i) {
      StringBuffer buffer = new StringBuffer("LineNumber");
      buffer.append(i);
      changedLines.add(buffer.toString());
    }

    EditRequest editRequest = new EditRequest(35, 70, changedLines, fileName, new Cursor(0,0));
    sourceFileHandlerHybrid.editLines(editRequest);

    PageRequest pageRequest = new PageRequest(0, fileName, N, new Cursor(0,0));
    Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    assertEquals(fileInfo.getLines().subList(0, 35), page.getLines().subList(0,35));
    assertEquals(changedLines, page.getLines().subList(35, 70));
    assertEquals(fileInfo.getLines().subList(70, N), page.getLines().subList(70,N));
  }


  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void insertLinesAtTop() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 10;
    int K = 3;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    List<String> changedLines = new ArrayList<>();
    for (int i = 0; i < K; ++i) {
      StringBuffer buffer = new StringBuffer("LineNumber");
      buffer.append(i);
      changedLines.add(buffer.toString());
    }
    List<String> newContents = new ArrayList<>();

    newContents.addAll(changedLines);
    newContents.addAll(fileInfo.getLines());

    Cursor cursor = new Cursor(0, 0);
    EditRequest editRequest = new EditRequest( 0, N, newContents, fileName, cursor);
    sourceFileHandlerHybrid.editLines(editRequest);

    PageRequest pageRequest = new PageRequest(0, fileName, N + K, new Cursor(0,0));
    Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    assertEquals(newContents, page.getLines());
    assertEquals(0, page.getStartingLineNo());
    assertEquals(cursor, page.getCursorAt());
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void insertLinesAtBottom() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 10;
    int K = 3;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    List<String> changedLines = new ArrayList<>();
    for (int i = 0; i < K; ++i) {
      StringBuffer buffer = new StringBuffer("LineNumber");
      buffer.append(i);
      changedLines.add(buffer.toString());
    }
    List<String> newContents = new ArrayList<>();

    newContents.addAll(fileInfo.getLines());
    newContents.addAll(changedLines);

    Cursor cursor = new Cursor(0, 0);
    EditRequest editRequest = new EditRequest( 0, N, newContents, fileName, cursor);
    sourceFileHandlerHybrid.editLines(editRequest);

    PageRequest pageRequest = new PageRequest(0, fileName, N + K, new Cursor(0,0));
    Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    assertEquals(newContents, page.getLines());
    assertEquals(0, page.getStartingLineNo());
    assertEquals(cursor, page.getCursorAt());
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void randomInsertUpdateDelete() {
    int seed = 0x1231;
    Random random = new Random(seed);
    String fileName = "randomInsertUpdateDelete";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 1000;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    List<String> newContents = new ArrayList<>();
    newContents.addAll(fileInfo.getLines());

    int K = N;
    for (int i = 0; i < N; ++i) {
      int len = newContents.size();
      int toss= random.nextInt(3);
      int index = random.nextInt(len);
      if (toss < 0) {
        newContents.remove(index);
      } else if (toss < 1) {
        newContents.add(index, "Text to be inserted");
      } else {
        newContents.set(index, "Something else " + newContents.get(index) + "Something");
      }

      Cursor cursor = new Cursor(0, 0);
      EditRequest editRequest = new EditRequest( 0, K, newContents, fileName, cursor);
      sourceFileHandlerHybrid.editLines(editRequest);

      PageRequest pageRequest = new PageRequest(0, fileName, newContents.size(), new Cursor(0,0));
      Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

      K = page.getLines().size();

      assertEquals(newContents, page.getLines());
      assertEquals(0, page.getStartingLineNo());
      assertEquals(cursor, page.getCursorAt());
    }
  }


  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void editTotheTopOftheFileShouldBeVeryEfficient() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 10000;
    int K = 1000;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    List<String> changedLines = new ArrayList<>();
    for (int i = 0; i < 1; ++i) {
      StringBuffer buffer = new StringBuffer("LINENO");
      buffer.append(i);
      changedLines.add(buffer.toString());
    }

    Cursor cursorAt = new Cursor(0, 0);
    EditRequest editRequest = new EditRequest(0, 0, changedLines, fileName, cursorAt);
    PageRequest pageRequest = new PageRequest(0, fileName, 1, cursorAt);

    long startTime = System.currentTimeMillis();
    for (int i = 0; i < K; ++i) {
      sourceFileHandlerHybrid.editLines(editRequest);

      Page pageResponse = sourceFileHandlerHybrid.getLinesFrom(pageRequest);
      assertEquals(changedLines, pageResponse.getLines());
    }
    long timeTaken = (System.currentTimeMillis() - startTime);

    System.out.println(timeTaken);
    assert(timeTaken < 100);
    PageRequest pageRequestNew = new PageRequest(0, fileName, N + K, cursorAt);
    Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequestNew);

    assertEquals(fileInfo.getLines(), page.getLines().subList(K, K + N));
  }


  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void searchReplace() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 100;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    SearchReplaceRequest searchReplaceRequest = new SearchReplaceRequest(0, 0, "lineno",
        "LineNumber", fileName);

    sourceFileHandlerHybrid.searchReplace(searchReplaceRequest);

    List<String> expected = new ArrayList<>();
    for (int i = 0; i < N; ++i) {
      StringBuffer buffer = new StringBuffer("LineNumber");
      buffer.append(i);
      expected.add(buffer.toString());
    }

    PageRequest pageRequest = new PageRequest(0, fileName, 100, new Cursor(0,0));
    Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    assertEquals(expected, page.getLines());
  }


  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void findAfterSearchReplaceTest1() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 100;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    SearchReplaceRequest searchReplaceRequest = new SearchReplaceRequest(0, 0, "lineno",
        "LineNumber", fileName);

    final SearchRequest searchRequestBeforeReplace = new SearchRequest(0, "lineno", fileName);
    List<Cursor> cursors = sourceFileHandlerHybrid.search(searchRequestBeforeReplace);

    sourceFileHandlerHybrid.searchReplace(searchReplaceRequest);


    final SearchRequest searchRequestAfterReplace = new SearchRequest(0, "LineNumber", fileName);
    List<Cursor> cursorsAfterReplace =
        sourceFileHandlerHybrid.search(searchRequestAfterReplace);

    assertEquals(cursors, cursorsAfterReplace);
    List<String> expected = new ArrayList<>();
    for (int i = 0; i < N; ++i) {
      StringBuffer buffer = new StringBuffer("LineNumber");
      buffer.append(i);
      expected.add(buffer.toString());
    }

    PageRequest pageRequest = new PageRequest(0, fileName, 100, new Cursor(0,0));
    Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    assertEquals(expected, page.getLines());
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void findAfterSearchReplaceTest2() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 100;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    SearchReplaceRequest searchReplaceRequest = new SearchReplaceRequest(0, 0, "line",
        "awesome", fileName);

    sourceFileHandlerHybrid.searchReplace(searchReplaceRequest);

    List<Cursor> expectedPositions = new ArrayList<>();
    for (int i = 0; i < N; ++i) {
      expectedPositions.add(new Cursor(i, 7));
    }

    final SearchRequest searchRequestAfterReplace = new SearchRequest(0, "no", fileName);
    List<Cursor> cursorsAfterReplace =
        sourceFileHandlerHybrid.search(searchRequestAfterReplace);

    assertEquals(expectedPositions, cursorsAfterReplace);
    List<String> expected = new ArrayList<>();
    for (int i = 0; i < N; ++i) {
      StringBuffer buffer = new StringBuffer("awesomeno");
      buffer.append(i);
      expected.add(buffer.toString());
    }

    PageRequest pageRequest = new PageRequest(0, fileName, 100, new Cursor(0,0));
    Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    assertEquals(expected, page.getLines());
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void SearchForNonEmptyStringAndReplaceWithEmptyString() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 100;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    SearchReplaceRequest searchReplaceRequest = new SearchReplaceRequest(0, 0, "line",
        "", fileName);

    sourceFileHandlerHybrid.searchReplace(searchReplaceRequest);

    List<Cursor> expectedPositions = new ArrayList<>();
    for (int i = 0; i < N; ++i) {
      expectedPositions.add(new Cursor(i, 0));
    }

    final SearchRequest searchRequestAfterReplace = new SearchRequest(0, "no", fileName);
    List<Cursor> cursorsAfterReplace =
        sourceFileHandlerHybrid.search(searchRequestAfterReplace);

    assertEquals(expectedPositions, cursorsAfterReplace);
    List<String> expected = new ArrayList<>();
    for (int i = 0; i < N; ++i) {
      StringBuffer buffer = new StringBuffer("no");
      buffer.append(i);
      expected.add(buffer.toString());
    }

    PageRequest pageRequest = new PageRequest(0, fileName, 100, new Cursor(0,0));
    Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    assertEquals(expected, page.getLines());
  }



  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void undoSearchReplace() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 100;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    SearchReplaceRequest searchReplaceRequest = new SearchReplaceRequest(0, 0, "lineno",
        "LineNumber", fileName);

    sourceFileHandlerHybrid.searchReplace(searchReplaceRequest);

    List<String> expected = new ArrayList<>();
    for (int i = 0; i < N; ++i) {
      StringBuffer buffer = new StringBuffer("LineNumber");
      buffer.append(i);
      expected.add(buffer.toString());
    }

    PageRequest pageRequest = new PageRequest(0, fileName, 100, new Cursor(0,0));
    Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    assertEquals(expected, page.getLines());

    UndoRequest undoRequest = new UndoRequest(fileName);
    sourceFileHandlerHybrid.undo(undoRequest);

    Page pageAfterUndo = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    assertEquals(fileInfo.getLines(), pageAfterUndo.getLines());
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void undoEditLines() {
    String fileName = "testfile";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 100;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    List<String> changedLines = new ArrayList<>();
    for (int i = 0; i < 35; ++i) {
      StringBuffer buffer = new StringBuffer("LineNumber");
      buffer.append(i);
      changedLines.add(buffer.toString());
    }

    EditRequest editRequest = new EditRequest(35, 70, changedLines, fileName, new Cursor(0,0));
    sourceFileHandlerHybrid.editLines(editRequest);

    PageRequest pageRequest = new PageRequest(0, fileName, N, new Cursor(0,0));
    Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    assertEquals(fileInfo.getLines().subList(0, 35), page.getLines().subList(0,35));
    assertEquals(changedLines, page.getLines().subList(35, 70));
    assertEquals(fileInfo.getLines().subList(70, N), page.getLines().subList(70,N));

    UndoRequest undoRequest = new UndoRequest(fileName);
    sourceFileHandlerHybrid.undo(undoRequest);
    Page pageAfterUndo = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    assertEquals(fileInfo.getLines(), pageAfterUndo.getLines());
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void onUndoTheCursorShouldBeReturnedToRightPosition() {
    String fileName = "randomInsertUpdateDelete";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);
    int N = 200;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    final Cursor cursor = new Cursor(100, 5);
    List<String> content = new ArrayList<>();
    content.add("LINENUMBER100");
    final EditRequest editRequest = new EditRequest(100, 101, content, fileName, cursor);
    sourceFileHandlerHybrid.editLines(editRequest);

    final SearchReplaceRequest searchReplaceRequest = new SearchReplaceRequest(0, 100, "LINENUMBER",
        "lineno", fileName);

    final PageRequest pageRequest = new PageRequest(0, fileName, 200, cursor);
    Page pageAfterUpdate = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    assertEquals(content, pageAfterUpdate.getLines().subList(100, 101));
    assertEquals(fileInfo.getLines().subList(0, 100), pageAfterUpdate.getLines().subList(0, 100));

    sourceFileHandlerHybrid.searchReplace(searchReplaceRequest);
    Page pageAfterSearchReplace = sourceFileHandlerHybrid.getLinesFrom(pageRequest);
    assertEquals(fileInfo.getLines(), pageAfterSearchReplace.getLines());
    final UndoRequest undoRequest = new UndoRequest(fileName);
    sourceFileHandlerHybrid.undo(undoRequest);
    Page pageAfterFirstUndo = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    assertEquals(content, pageAfterFirstUndo.getLines().subList(100, 101));
    assertEquals(fileInfo.getLines().subList(0, 100), pageAfterFirstUndo.getLines().subList(0, 100));

    sourceFileHandlerHybrid.undo(undoRequest);
    Page pageAfterSecondUndo = sourceFileHandlerHybrid.getLinesFrom(pageRequest);


    assertEquals(fileInfo.getLines(), pageAfterSecondUndo.getLines());
    assertEquals(new Cursor(0, 0), pageAfterSecondUndo.getCursorAt());
  }

//  List<String> searchReplaceString(List<String> lst, String pattern, String newPattern) {
//
//  }
@Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
void sameContentUpdateDoesNotIncrementVersionTest1() {
  int seed = 0x1231;
  Random random = new Random(seed);
  String fileName = "randomInsertUpdateDelete";
  SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

  int N = 100;
  FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
  sourceFileHandlerHybrid.loadFile(fileInfo);

  final Cursor cursor = new Cursor(0, 0);
  final EditRequest editRequest = new EditRequest(0, 10, clone(fileInfo.getLines().subList(0, 10)),
      fileName, cursor);

  sourceFileHandlerHybrid.editLines(editRequest);
  final PageRequest pageRequest = new PageRequest(0, fileName, 100, cursor);
  final Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

  assertEquals(fileInfo.getLines(), page.getLines());
  assertEquals(0, page.getStartingLineNo());
  assertEquals(cursor, page.getCursorAt());

  final UndoRequest undoRequest = new UndoRequest(fileName);
  sourceFileHandlerHybrid.undo(undoRequest);

  Page pageAfterUndo = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

  assertEquals(fileInfo.getLines(), pageAfterUndo.getLines());
  assertEquals(0, pageAfterUndo.getStartingLineNo());
  assertEquals(cursor, pageAfterUndo.getCursorAt());
}

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void sameContentUpdateDoesNotIncrementVersionTest2() {
    int seed = 0x1231;
    Random random = new Random(seed);
    String fileName = "randomInsertUpdateDelete";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 100;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    final Cursor cursor = new Cursor(0, 0);
    final SearchReplaceRequest searchReplaceRequest = new SearchReplaceRequest(0, 100, "lineno",
        "lineno", fileName);
    sourceFileHandlerHybrid.searchReplace(searchReplaceRequest);
    final PageRequest pageRequest = new PageRequest(0, fileName, 100, cursor);
    final Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    assertEquals(fileInfo.getLines(), page.getLines());
    assertEquals(0, page.getStartingLineNo());
    assertEquals(cursor, page.getCursorAt());

    final UndoRequest undoRequest = new UndoRequest(fileName);
    sourceFileHandlerHybrid.undo(undoRequest);

    Page pageAfterUndo = sourceFileHandlerHybrid.getLinesFrom(pageRequest);

    assertEquals(fileInfo.getLines(), pageAfterUndo.getLines());
    assertEquals(0, pageAfterUndo.getStartingLineNo());
    assertEquals(cursor, pageAfterUndo.getCursorAt());
  }

  @Test
  @Timeout(value = 60000, unit = TimeUnit.MILLISECONDS)
  void randomUndoRedoTest() {
    int seed = 0x1231;
    Random random = new Random(seed);
    String fileName = "randomInsertUpdateDelete";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 750;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    List<String> newContents = new ArrayList<>();
    newContents.addAll(fileInfo.getLines());

    List<List<String>> fileVersions = new ArrayList<>();
    List<Integer> sizes = new ArrayList<>();
    int K = N;
    fileVersions.add(clone(newContents));
    sizes.add(K);

    for (int i = 0; i < N; ++i) {
      int len = newContents.size();
      int toss= random.nextInt(3) % 3;
      int index = random.nextInt(len);
      if (toss == 0) {
        newContents.remove(index);
      } else if (toss == 1) {
        newContents.add(index, "Text to be inserted");
      } else {
        newContents.set(index, "pre " + newContents.get(index) + " post");
      }

      List<String> clonedList = clone(newContents);
      fileVersions.add(clonedList);

      Cursor cursor = new Cursor(0, 0);
      EditRequest editRequest = new EditRequest( 0, K, clone(newContents), fileName, cursor);
      sourceFileHandlerHybrid.editLines(editRequest);

      PageRequest pageRequest = new PageRequest(0, fileName, newContents.size(), new Cursor(0,0));
      Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);
      K = page.getLines().size();
      sizes.add(newContents.size());
    }

    Deque<Integer> dq = new LinkedList<>();
    final UndoRequest undoRequest = new UndoRequest(fileName);
    Cursor cursor = new Cursor(0, 0);

    int undoIndex = fileVersions.size() - 2;
    for (int i = 0; i < N; ++i) {
      List<String> thisVersion = fileVersions.get(Math.max(0, undoIndex));
      Integer sz = sizes.get(Math.max(0, undoIndex));

      int toss = random.nextInt(2) % 2;
      if (toss == 0) {
        sourceFileHandlerHybrid.undo(undoRequest);
        dq.addLast(undoIndex);
        --undoIndex;
      } else {
        if (dq.size() > 0) {
          sourceFileHandlerHybrid.redo(undoRequest);
          undoIndex = dq.getLast();
          dq.removeLast();
        }
        int idx = Math.min(undoIndex + 1, fileVersions.size()-1);
        thisVersion = fileVersions.get(idx);
        sz = sizes.get(idx);
      }

      PageRequest pageRequest = new PageRequest(0, fileName, sz, cursor);
      Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);
      assertEquals(thisVersion, page.getLines());
    }
  }

  @Test
  @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
  void nothingToUndoReturnsLoadedContentTest() {
    int seed = 0x1231;
    Random random = new Random(seed);
    String fileName = "randomInsertUpdateDelete";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 100;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    List<String> newContents = new ArrayList<>();
    newContents.addAll(fileInfo.getLines());

    List<List<String>> fileVersions = new ArrayList<>();
    List<Integer> sizes = new ArrayList<>();
    int K = N;
    int R = 15;
    for (int i = 0; i < R; ++i) {
      fileVersions.add(clone(newContents));
      sizes.add(K);
    }

    for (int i = 0; i < N; ++i) {
      int len = newContents.size();
      int toss= random.nextInt() % 3;
      int index = random.nextInt(len);
      if (toss == 0) {
        newContents.remove(index);
      } else if (toss == 1) {
        newContents.add(index, "Text to be inserted");
      } else {
        newContents.set(index, "pre " + newContents.get(index) + " post");
      }

      List<String> clonedList = clone(newContents);
      fileVersions.add(clonedList);

      Cursor cursor = new Cursor(0, 0);
      EditRequest editRequest = new EditRequest( 0, K, clone(newContents), fileName, cursor);
      sourceFileHandlerHybrid.editLines(editRequest);

      PageRequest pageRequest = new PageRequest(0, fileName, newContents.size(), new Cursor(0,0));
      Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);
      K = page.getLines().size();
      sizes.add(newContents.size());
    }

    final UndoRequest undoRequest = new UndoRequest(fileName);
    Cursor cursor = new Cursor(0, 0);

    for (int i = fileVersions.size() - 2 ; i >= 0; --i) {
      List<String> thisVersion = fileVersions.get(i);
      Integer sz = sizes.get(i);
      sourceFileHandlerHybrid.undo(undoRequest);
      PageRequest pageRequest = new PageRequest(0, fileName, sz, cursor);

      Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);
      assertEquals(thisVersion, page.getLines());
    }
  }

  @Test
  @Timeout(value = 20000, unit = TimeUnit.MILLISECONDS)
  void randomUpdatesAndUndoTest() {
    int seed = 0x1231;
    Random random = new Random(seed);
    String fileName = "randomInsertUpdateDelete";
    SourceFileHandler sourceFileHandlerHybrid = getSourceFileHandler(fileName);

    int N = 500;
    FileInfo fileInfo = getLargeSampleFileInfo(fileName, N);
    sourceFileHandlerHybrid.loadFile(fileInfo);

    List<String> newContents = new ArrayList<>();
    newContents.addAll(fileInfo.getLines());

    List<List<String>> fileVersions = new ArrayList<>();
    List<Integer> sizes = new ArrayList<>();
    int K = N;
    fileVersions.add(clone(newContents));
    sizes.add(K);

    for (int i = 0; i < N; ++i) {
      int len = newContents.size();
      int toss= random.nextInt() % 3;
      int index = random.nextInt(len);
      if (toss == 0) {
        newContents.remove(index);
      } else if (toss == 1) {
        newContents.add(index, "Text to be inserted");
      } else {
        newContents.set(index, "pre " + newContents.get(index) + " post");
      }

      List<String> clonedList = clone(newContents);
      fileVersions.add(clonedList);

      Cursor cursor = new Cursor(0, 0);
      EditRequest editRequest = new EditRequest( 0, K, clone(newContents), fileName, cursor);
      sourceFileHandlerHybrid.editLines(editRequest);

      PageRequest pageRequest = new PageRequest(0, fileName, newContents.size(), new Cursor(0,0));
      Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);
      K = page.getLines().size();
      sizes.add(newContents.size());
    }

    final UndoRequest undoRequest = new UndoRequest(fileName);
    Cursor cursor = new Cursor(0, 0);

    assert (fileVersions.size() == N + 1);
    for (int i = fileVersions.size() - 2 ; i >= 0; --i) {
      List<String> thisVersion = fileVersions.get(i);
      Integer sz = sizes.get(i);
      sourceFileHandlerHybrid.undo(undoRequest);
      PageRequest pageRequest = new PageRequest(0, fileName, sz, cursor);

      Page page = sourceFileHandlerHybrid.getLinesFrom(pageRequest);
      assertEquals(thisVersion, page.getLines());
    }
  }
  @Test
  void getCursorPage() {
    // FIXME: important for the frontend to work
  }

}
