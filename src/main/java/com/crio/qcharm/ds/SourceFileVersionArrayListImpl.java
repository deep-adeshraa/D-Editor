package com.crio.qcharm.ds;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import com.crio.qcharm.request.PageRequest;
import com.crio.qcharm.request.SearchRequest;

import org.apache.commons.lang3.StringUtils;

public class SourceFileVersionArrayListImpl implements SourceFileVersion {

  String filename;
  ArrayList<String> lines = new ArrayList<>();

  public SourceFileVersionArrayListImpl(SourceFileVersionArrayListImpl obj) {
    this.filename = obj.filename;
    this.lines = new ArrayList<>(obj.lines);
  }

  public SourceFileVersionArrayListImpl() {
  }

  public SourceFileVersionArrayListImpl(FileInfo fileInfo) {
    this.filename = fileInfo.getFileName();
    this.lines = new ArrayList<>(fileInfo.getLines());
  }

  public SourceFileVersion apply(List<Edits> edits) {
    List<String> lines = new ArrayList<>();
    lines.addAll(lines);

    //SourceFileVersionArrayListImpl latest = new SourceFileVersionArrayListImpl();

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

  public void apply(SearchReplace searchReplace) {
    List<Cursor> answer = new ArrayList<>();
    answer = PatternSearchAlgorithm.searchPattern(searchReplace.getPattern().toCharArray(), this.lines);
    TreeSet<Cursor> ts = new TreeSet<>(Comparator.comparing(Cursor::getLineNo));
    ts.addAll(answer);
    for (Cursor c : ts) {
      String temp = lines.get(c.getLineNo());
      temp = StringUtils.replace(temp, searchReplace.getPattern(), searchReplace.getNewPattern());
      lines.set(c.getLineNo(), temp);
    }
  }

  public void apply(UpdateLines updateLines) {
    int start = updateLines.getStartingLineNo();
    int end = start + updateLines.getNumberOfLines();
    List<String> subLines;
    try {
      subLines = this.lines.subList(start, end + 1);
    } catch (Exception e) {
      subLines = this.lines.subList(start, start);
    }
    subLines.clear();
    subLines.addAll(updateLines.getLines());
  }

  @Override
  public Page getLinesBefore(PageRequest pageRequest) {
    int lineNumber = pageRequest.getStartingLineNo();
    int numberOfLines = pageRequest.getNumberOfLines();
    if (lineNumber == 0) {
      return new Page(new ArrayList<String>(), 0, pageRequest.getFileName(), pageRequest.getCursorAt());
    } else if (lineNumber - numberOfLines < 0) {
      return new Page(lines.subList(0, lineNumber), 0, pageRequest.getFileName(), pageRequest.getCursorAt());
    }
    return new Page(lines.subList(lineNumber - numberOfLines, lineNumber), lineNumber - numberOfLines,
        pageRequest.getFileName(), pageRequest.getCursorAt());
  }

  @Override
  public Page getLinesAfter(PageRequest pageRequest) {
    int lineNumber = pageRequest.getStartingLineNo();
    int numberOfLines = pageRequest.getNumberOfLines();
    if (lineNumber >= lines.size()) {
      return new Page(new ArrayList<String>(), lineNumber, pageRequest.getFileName(), pageRequest.getCursorAt());
    }
    else if (lineNumber + numberOfLines + 1 > lines.size()) {
      return new Page(this.lines.subList(lineNumber + 1, this.lines.size()), lineNumber + 1, pageRequest.getFileName(),
          pageRequest.getCursorAt());
    }
    return new Page(lines.subList(lineNumber + 1, Math.min(lineNumber + numberOfLines + 1, this.lines.size())),
        lineNumber + 1, pageRequest.getFileName(), pageRequest.getCursorAt());
  }

  @Override
  public Page getLinesFrom(PageRequest pageRequest) {
    int lineNumber = pageRequest.getStartingLineNo();
    int numberOfLines = pageRequest.getNumberOfLines();
   return new Page(
      lines.subList(lineNumber,
          lines.size() < lineNumber + numberOfLines ? lines.size() : lineNumber + numberOfLines),
      lineNumber, pageRequest.getFileName(), new Cursor(lineNumber, 0));
  }

  public List<Cursor> getCursors(SearchRequest searchRequest) {
    List<Cursor> answer = new ArrayList<>();
    try {
      answer = PatternSearchAlgorithm.searchPattern(searchRequest.getPattern().toCharArray(), this.lines);
    } catch (Exception e) {
      return new ArrayList<>();
    }
    return answer;
  }

  @Override
  public List<String> getAllLines() {
    return this.lines;
  }

  @Override
  public Page getCursorPage() {
    return null;
  }

}
