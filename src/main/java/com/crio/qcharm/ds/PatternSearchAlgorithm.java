package com.crio.qcharm.ds;

import java.util.LinkedList;
import java.util.List;

class PatternSearchAlgorithm {
    static void computeLPSArray(char[] pat, int M, int lps[]) {
        // length of the previous longest prefix suffix
        int len = 0;
        int i = 1;
        lps[0] = 0; // lps[0] is always 0

        // the loop calculates lps[i] for i = 1 to M-1
        while (i < M) {
            if (pat[i] == pat[len]) {
                len++;
                lps[i] = len;
                i++;
            } else // (pat[i] != pat[len])
            {
                // This is tricky. Consider the example.
                // AAACAAAA and i = 7. The idea is similar
                // to search step.
                if (len != 0) {
                    len = lps[len - 1];

                    // Also, note that we do not increment
                    // i here
                } else // if (len == 0)
                {
                    lps[i] = len;
                    i++;
                }
            }
        }
    }

    static LinkedList<Cursor> searchPattern(char[] pat, List<String> s) {
        LinkedList<Cursor> ans = new LinkedList<>();
        int M = pat.length;
        int lps[] = new int[M];
        int count = 0;
        for (String txt : s) {
            int N = txt.length();
            char[] txtaray = txt.toCharArray();
            int j = 0;
            computeLPSArray(pat, M, lps);
            int i = 0;
            while (i < N) {
                if (pat[j] == txtaray[i]) {
                    j++;
                    i++;
                }
                if (j == M) {
                    /////////////////////////////////////
                    ans.add(new Cursor(count, i - j));
                    /////////////////////////////////////
                    j = lps[j - 1];
                } else if (i < N && pat[j] != txtaray[i]) {
                    if (j != 0)
                        j = lps[j - 1];
                    else
                        i = i + 1;
                }
            }
            count++;
        }
        return ans;
    }

}
// 1911910294
// 1935454818
// 1900647144
// 1888197322
// 3300297132 = z algo
// 4004468035 = beyer
// 1664638675
// 1631045311 = kmp
// 1352545784 = kmp with charArray
// 1500000000 = required