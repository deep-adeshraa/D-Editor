package com.crio.qcharm.ds;

import com.crio.qcharm.request.PageRequest;
import com.crio.qcharm.request.SearchRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import lombok.Data;

@Data
public class SourceFileVersionHybridImpl implements SourceFileVersion {

  private FileInfo fileInfo;
  private List<String> lines;

  public SourceFileVersionHybridImpl(FileInfo fileInfo) {
    this.fileInfo = fileInfo;
    this.lines = new ArrayList<>(fileInfo.lines);
  }

  public SourceFileVersionHybridImpl() {
  }

  public SourceFileVersionHybridImpl(SourceFileVersionHybridImpl obj) {
    this.fileInfo = obj.fileInfo;
    this.lines = new ArrayList<>(obj.lines);
  }

  @Override
  public SourceFileVersion apply(List<Edits> edits) {

    for (Edits oneEdit : edits) {
      if (oneEdit instanceof UpdateLines) {
        apply((UpdateLines) oneEdit);
      } else {
        assert (oneEdit instanceof SearchReplace);
        apply((SearchReplace) oneEdit);
      }
    }
    return this;
  }

  @Override
  public void apply(SearchReplace searchReplace) {
    SearchRequest request = new SearchRequest(searchReplace.getStartingLineNo(), searchReplace.getPattern(), null);
    Set<Cursor> cursors = new TreeSet<>(Comparator.comparingInt(Cursor::getLineNo));
    // List<Cursor> cursors = this.getCursors(request);
    cursors.addAll(this.getCursors(request));
    // LinkedList<String> lines = this.lines;
    cursors.forEach(cursor -> {
      String str = this.lines.get(cursor.getLineNo());
      str = str.replace(searchReplace.getPattern(), searchReplace.getNewPattern());
      lines.set(cursor.getLineNo(), str);
    });

  }

  @Override
  public void apply(UpdateLines updateLines) {
    int start = updateLines.getStartingLineNo();
    int end = start + updateLines.getNumberOfLines();
    // List<String> lines = new LinkedList<>(this.lines);
    List<String> subLines = lines.subList(start, end + 1 > lines.size() ? end : end + 1);
    subLines.clear();
    subLines.addAll(updateLines.getLines());
  }

  @Override
  public List<String> getAllLines() {
    return this.lines;
  }

  @Override
  public Page getLinesBefore(PageRequest pageRequest) {
    int lineNumber = pageRequest.getStartingLineNo();
    int numberOfLines = pageRequest.getNumberOfLines();
    List<String> lines = this.lines;
    Page page = new Page();
    page.setFileName(pageRequest.getFileName());
    page.setCursorAt(pageRequest.getCursorAt());
    page.setStartingLineNo(lineNumber - numberOfLines >= 0 ? lineNumber - numberOfLines : 0);
    if (lineNumber == 0)
      page.setLines(new ArrayList<>());
    else if (lineNumber == lines.size())
      page.setLines(new ArrayList<>());
    else
      page.setLines(lines.subList((lineNumber - numberOfLines) < 0 ? 0 : lineNumber - numberOfLines, lineNumber));
    return page;
  }

  @Override
  public Page getLinesAfter(PageRequest pageRequest) {
    int lineNumber = pageRequest.getStartingLineNo();
    int numberOfLines = pageRequest.getNumberOfLines();
    List<String> lines = this.lines;
    Page page = new Page();
    page.setFileName(pageRequest.getFileName());
    page.setCursorAt(pageRequest.getCursorAt());
    page.setStartingLineNo(lineNumber + 1);
    if (lineNumber == lines.size()) {
      page.setLines(new ArrayList<>());
      page.setStartingLineNo(lineNumber);
    } else
      page.setLines(lines.subList(lineNumber + 1,
          (lineNumber + numberOfLines) > lines.size() ? lines.size() : lineNumber + numberOfLines + 1));
    return page;
  }

  @Override
  public Page getLinesFrom(PageRequest pageRequest) {
    int lineNumber = pageRequest.getStartingLineNo();
    int numberOfLines = pageRequest.getNumberOfLines();
    List<String> lines = this.lines;
    Page page = new Page();
    page.setFileName(pageRequest.getFileName());
    page.setCursorAt(new Cursor(lineNumber, 0));
    page.setStartingLineNo(lineNumber);
    page.setLines(lines.subList(lineNumber,
        (lineNumber + numberOfLines) > lines.size() ? lines.size() : lineNumber + numberOfLines));
    return page;
  }

  @Override
  public List<Cursor> getCursors(SearchRequest searchRequest) {
    // boolean efficient = true;
    if (searchRequest.getPattern().isEmpty()) {
      return new ArrayList<>();
    }
    return PatternSearchAlgorithm.searchPattern(searchRequest.getPattern().toCharArray(), this.lines);
  }

  @Override
  public Page getCursorPage() {
    return null;
  }

}