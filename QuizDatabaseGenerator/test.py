#!/usr/bin/python
# -*- coding: utf-8 -*-

from __future__ import print_function
import re
import sys
import codecs
import os
import sqlite3

DB = 'hamtest.db'

schemes = [
    ("CREATE TABLE IF NOT EXISTS categories \
    (ID INTEGER PRIMARY KEY AUTOINCREMENT, level INTEGER, qnum INTEGER, max_errors INTEGER)"),
    ("CREATE TABLE IF NOT EXISTS images \
    (ID INTEGER PRIMARY KEY AUTOINCREMENT, image BLOB, idx INTEGER)"),
    ("CREATE TABLE IF NOT EXISTS questions \
    (ID INTEGER PRIMARY KEY AUTOINCREMENT, question_text TEXT, question_image INTEGER, number INTEGER, category INTEGER, use_count INTEGER, last_result INTEGER)"),
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
    file = 'text/' + str(category) + '.txt'
    print('Opening {0}'.format(file))
    with codecs.open(file, "r", "utf_8_sig" ) as f:
        return f.read().splitlines()


def getNumberOfQuestions(test):
    number = 0
    for k in test:
        if u"Вопрос" in k:
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
        if u"Вопрос" in s:
            num = num + 1
            print('Вопрос {0}'.format(num))
            question = ''
            it = 1
            while test[i + it] and u"a)    " not in test[i + it] and u"<<" not in test[i + it]:
                question = question + " " + test[i + it]
                it = it + 1
            print(question)
            c.execute(
                "INSERT OR REPLACE INTO questions(question_text, number, category, question_image, use_count) VALUES (?,?,?,?,?)",
                (question, num, category, 0, 0))
            quest_id = c.lastrowid
            continue
        if "<<" in s:
            result = re.search('<<(.*)>>', s)
            c.execute("update questions set question_image=? where ID=?", (result.group(1), quest_id,))
            continue
        if u"a)    " in s:
            print(s[6:])
            c.execute("INSERT OR REPLACE INTO answers(answer_text, is_correct, serial, question) VALUES (?,?,?,?)",
                      (s[6:], 0, 1, quest_id))
            continue
        if u"b)    " in s:
            print(s[6:])
            c.execute("INSERT OR REPLACE INTO answers(answer_text, is_correct, serial, question) VALUES (?,?,?,?)",
                      (s[6:], 0, 2, quest_id))
            continue
        if u"c)    " in s:
            print(s[6:])
            c.execute("INSERT OR REPLACE INTO answers(answer_text, is_correct, serial, question) VALUES (?,?,?,?)",
                      (s[6:], 0, 3, quest_id))
            continue
        if u"d)    " in s:
            print(s[6:])
            c.execute("INSERT OR REPLACE INTO answers(answer_text, is_correct, serial, question) VALUES (?,?,?,?)",
                      (s[6:], 0, 4, quest_id))
            continue
    conn.commit()
    conn.close()


def processImages():
    conn = sqlite3.connect(DB)
    c = conn.cursor()
    for subdir, dirs, files in os.walk(os.getcwd()):
        for file in files:
            # print os.path.join(subdir, file)
            filepath = subdir + os.sep + file

            if filepath.endswith(".png"):
                imageFile = open(filepath, 'rb')
                b = sqlite3.Binary(imageFile.read())
                c.execute("INSERT INTO images (image, idx) values(?,?)", (b, os.path.splitext(file)[0],))
    conn.commit()
    conn.close()


def process_answers(category):
    with open('text/' + str(category) + 'a.txt') as f:
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

            sql = '''update answers set is_correct=1 where question=\
            (select ID from questions where category={0} and number={1}) and  serial={2}'''.format(category, quest, result)
            quest = quest + 1
            c.execute(sql)
        conn.commit()
        conn.close()


def main():

    create_db()
    setPassConditions()
    processImages()
    for i in range(2, 5, 1):
        print('Processing category {0}'.format(i))
        lines = process_tests(i)
        if lines:
            print('Number of questions: {0}'.format(getNumberOfQuestions(lines)))
            extract_questions(lines, i)
            process_answers(i)

if __name__ == '__main__':
    main()
