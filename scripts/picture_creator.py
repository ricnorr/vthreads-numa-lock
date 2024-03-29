import pandas as pd
import matplotlib.pyplot as plt
import os
import glob
import re
import numpy as np
import sys
from matplotlib.backends.backend_pdf import PdfPages
import matplotlib.ticker as ticker

df = pd.read_csv('../results/benchmark_results.csv', index_col=None, header=0)
bench_names = sorted(set(df['name']))
locks = sorted(set(df['lock']))
# styles = ['--']
paths = []


def draw_by_bench_and_mode(df, bench, mode, threads, suffix, filterLocks):
        if 'Median of execution time (ms)' == mode:
            if 'Text' not in bench:
                return
        if 'Throughput (op|ms)' == mode and 'Text' in bench:
            return
        if 'Низкая' in bench and 'overhead' in mode:
            return
        ax = plt.axes(title = bench + '\n mode=' + mode)
        #threads = [4, 16, 24, 32, 48, 80, 128, 192, 256, 512, 1024, 1280, 2560]
        plt.gcf().set_size_inches(7, 7)
        ind = 0
        markevery = [1,2]
        for lock in locks:
            if len(filterLocks) != 1:
                if lock not in filterLocks:
                    continue
            cur_df = df[(df['name'] == bench) & (df['lock'] == lock) & (df['threads'].isin(threads))].copy()
            cur_df['threads_cnt'] = cur_df['threads'].map(str)
#             if lock == 'UNFAIR_REENTRANT' or lock == 'FAIR_REENTRANT' or lock == 'SYNCHRONIZED':
#                 marker = 'o'
#             else:
#                 marker = '.'
            cur_df.sort_values(by=['threads']).plot(ax=ax, x='threads_cnt', y=mode, label=lock)
            ind+=1
        l = plt.legend(loc='upper left')
        for text in l.get_texts():
            t = text.get_text()
#             if 'REENTRANT' not in t and t != 'SYNCHRONIZED':
#                 text.set_color("red")
            if 'Throughput' in mode:
                plt.ylabel('op/ms')
            else:
                plt.ylabel('ms')
        name = '../results/pictures/' + bench + '_' + mode  + suffix + '.png'
        paths.append(name)

        plt.savefig(name, bbox_inches="tight")
        plt.close()

for bench in bench_names:
    for mode in ['Throughput (op|ms)']:
        x = sorted(set(df['threads']))
        y = x[1:-1]
        draw_by_bench_and_mode(df, bench, mode, x, '_all', sys.argv)

from PIL import Image
im1 = Image.open(paths[0]).convert('RGB')
images = []
for i in range(1, len(paths)):
    images.append(Image.open(paths[i]).convert('RGB'))


im1.save(r'../results/result.pdf', save_all=True, append_images=images, dpi=(200, 200))
