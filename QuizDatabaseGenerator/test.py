#!/usr/bin/python
# -*- coding: utf-8 -*-

from __future__ import print_function
import re
import sys
import itertools
import os
import sqlite3

DB = 'main.db'

schemes = [
    ("CREATE TABLE IF NOT EXISTS categories \
    (ID INTEGER PRIMARY KEY AUTOINCREMENT, level INTEGER, qnum INTEGER, max_errors INTEGER)"),
    ("CREATE TABLE IF NOT EXISTS questions \
    (ID INTEGER PRIMARY KEY AUTOINCREMENT, question_text TEXT, number INTEGER, category INTEGER, use_count INTEGER, last_result INTEGER)"),
    ("CREATE TABLE IF NOT EXISTS answers \
    (ID INTEGER PRIMARY KEY AUTOINCREMENT, answer_text TEXT, is_correct INTEGER, serial INTEGER, question INTEGER)"),
]


def create_db():
    if os.path.isfile(DB):
        os.remove(DB)
    print("Opening database")
    conn = sqlite3.connect(DB)
    c = conn.cursor()
    c.execute("PRAGMA main.page_size = 4096")
    c.execute("PRAGMA main.cache_size=10000")
    c.execute("PRAGMA main.synchronous=OFF")
    c.execute("PRAGMA foreign_keys = OFF")
    c.execute("PRAGMA main.journal_mode=WAL")
    c.execute("PRAGMA main.cache_size=5000")
    c.execute("PRAGMA main.temp_store=MEMORY")

    print("Preparing database")
    for s in schemes:
        c.execute(s)
    conn.commit()
    conn.close()


def process_tests(category):
    with open(str(category) + '.txt') as f:
        return f.read().splitlines()


def getNumberOfQuestions(test):
    number = 0
    for k in test:
        if u"Вопрос" in k.decode('1251'):
            number = number + 1
    return number


def setPassConditions():
    conn = sqlite3.connect(DB)
    c = conn.cursor()
    c.execute("INSERT OR REPLACE INTO categories(level, qnum, max_errors) VALUES (?,?,?)",
                      (1, 45, 5))
    c.execute("INSERT OR REPLACE INTO categories(level, qnum, max_errors) VALUES (?,?,?)",
              (2, 30, 5))
    c.execute("INSERT OR REPLACE INTO categories(level, qnum, max_errors) VALUES (?,?,?)",
              (3, 25, 5))
    c.execute("INSERT OR REPLACE INTO categories(level, qnum, max_errors) VALUES (?,?,?)",
              (4, 20, 5))
    conn.commit()
    conn.close()


def extract_questions(test, category):
    num = 0
    conn = sqlite3.connect(DB)
    c = conn.cursor()
    quest_id = 0
    for i, s in enumerate(test):
        if u"Вопрос" in s.decode('1251'):
            num = num + 1
            print('Вопрос {0}'.format(num))
            question = ''
            it = 1
            while test[i + it] and u"a)    " not in test[i + it].decode('1251'):
                question = question + " " + test[i + it]
                it = it + 1
            print(question.decode('1251'))
            c.execute("INSERT OR REPLACE INTO questions(question_text, number, category) VALUES (?,?,?)",
                      (question.decode('1251'), num, category))
            quest_id = c.lastrowid
            continue
        if u"a)    " in s.decode('1251'):
            print(s[6:].decode('1251'))
            c.execute("INSERT OR REPLACE INTO answers(answer_text, is_correct, serial, question) VALUES (?,?,?,?)",
                      (s[6:].decode('1251'), 0, 1, quest_id))
            continue
        if u"b)    " in s.decode('1251'):
            print(s[6:].decode('1251'))
            c.execute("INSERT OR REPLACE INTO answers(answer_text, is_correct, serial, question) VALUES (?,?,?,?)",
                      (s[6:].decode('1251'), 0, 2, quest_id))
            continue
        if u"c)    " in s.decode('1251'):
            print(s[6:].decode('1251'))
            c.execute("INSERT OR REPLACE INTO answers(answer_text, is_correct, serial, question) VALUES (?,?,?,?)",
                      (s[6:].decode('1251'), 0, 3, quest_id))
            continue
        if u"d)    " in s.decode('1251'):
            print(s[6:].decode('1251'))
            c.execute("INSERT OR REPLACE INTO answers(answer_text, is_correct, serial, question) VALUES (?,?,?,?)",
                      (s[6:].decode('1251'), 0, 4, quest_id))
            continue
    conn.commit()
    conn.close()


def process_answers(category):
    with open(str(category) + 'a.txt') as f:
        answers = f.read().splitlines()
        quest = 1
        conn = sqlite3.connect(DB)
        c = conn.cursor()
        result = 0
        for a in answers:
            if "a" in a:
                result = 1
            elif "b" in a:
                result = 2
            elif "c" in a:
                result = 3
            elif "d" in a:
                result = 4
            else:
                sys.exit(1)

            sql = '''update answers set is_correct=1 where question=(select ID from questions where category={0} and number={1}) and  serial={2}'''.format(category,quest,result)
            quest = quest + 1
            c.execute(sql)
        conn.commit()
        conn.close()


def main():
    create_db()
    setPassConditions()
    for i in range(3, 5, 1):
        lines = process_tests(i)
        if lines:
            print('Number of questions: {0}'.format(getNumberOfQuestions(lines)))
            extract_questions(lines, i)
            process_answers(i)

if __name__ == '__main__':
    main()
